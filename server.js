// =================================================================
// ЧАСТЬ 1: НАСТРОЙКА BACKEND И БАЗЫ ДАННЫХ (POSTGRESQL)
// =================================================================
const express = require('express');
const { Pool } = require('pg');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware для обработки JSON-запросов
app.use(express.json());

// Подключение к базе данных Render PostgreSQL
// DATABASE_URL - это переменная окружения, которую предоставит Render
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
        rejectUnauthorized: false // Требуется для подключения к Render DB
    }
});

// Асинхронная функция для инициализации таблиц (создается при первом запуске)
async function initDb() {
    try {
        await pool.query(`
            CREATE TABLE IF NOT EXISTS tests (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                questions JSONB NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS results (
                id SERIAL PRIMARY KEY,
                test_id INTEGER REFERENCES tests(id),
                student_name VARCHAR(255) NOT NULL,
                student_group VARCHAR(255) NOT NULL,
                score INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        `);
        console.log('Database tables initialized.');
    } catch (err) {
        console.error('Error initializing database:', err);
    }
}
initDb();

// =================================================================
// ЧАСТЬ 2: МАРШРУТЫ API (BACKEND LOGIC)
// =================================================================

// 1. Создание нового теста
app.post('/api/tests', async (req, res) => {
    const { title, questions } = req.body;
    if (!title || !questions) {
        return res.status(400).send({ error: 'Title and questions are required.' });
    }
    try {
        const result = await pool.query(
            'INSERT INTO tests (title, questions) VALUES ($1, $2) RETURNING id',
            [title, questions]
        );
        // Возвращаем ID созданного теста, чтобы сформировать ссылку
        res.json({ 
            test_id: result.rows[0].id,
            link: `${req.protocol}://${req.get('host')}/quiz/${result.rows[0].id}`
        });
    } catch (err) {
        res.status(500).send({ error: 'Error creating test.' });
    }
});

// 2. Получение теста по ID
app.get('/api/tests/:id', async (req, res) => {
    try {
        const result = await pool.query('SELECT title, questions FROM tests WHERE id = $1', [req.params.id]);
        if (result.rows.length === 0) {
            return res.status(404).send({ error: 'Test not found.' });
        }
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).send({ error: 'Error fetching test.' });
    }
});

// 3. Сохранение результатов
app.post('/api/results', async (req, res) => {
    const { test_id, student_name, student_group, score } = req.body;
    if (!test_id || !student_name || !student_group || score === undefined) {
        return res.status(400).send({ error: 'Missing required fields for result.' });
    }
    try {
        await pool.query(
            'INSERT INTO results (test_id, student_name, student_group, score) VALUES ($1, $2, $3, $4)',
            [test_id, student_name, student_group, score]
        );
        res.status(201).send({ message: 'Results saved successfully.' });
    } catch (err) {
        res.status(500).send({ error: 'Error saving results.' });
    }
});

// 4. Получение результатов для преподавателя
app.get('/api/tests/:id/results', async (req, res) => {
    try {
        // Запрос с сортировкой по student_group для удобства преподавателя
        const result = await pool.query(
            'SELECT student_name, student_group, score, created_at FROM results WHERE test_id = $1 ORDER BY student_group, student_name',
            [req.params.id]
        );
        res.json(result.rows);
    } catch (err) {
        res.status(500).send({ error: 'Error fetching results.' });
    }
});

// =================================================================
// ЧАСТЬ 3: ФРОНТЕНД (ОДИН HTML-ФАЙЛ С JAVASCRIPT)
// =================================================================

// Главная страница / - будет использоваться преподавателем
app.get('/', (req, res) => {
    res.send(getTeacherHtml());
});

// Страница прохождения теста /quiz/:id - будет использоваться учеником
app.get('/quiz/:id', (req, res) => {
    res.send(getStudentQuizHtml(req.params.id));
});

// Запуск сервера
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

// =================================================================
// ЧАСТЬ 4: HTML-КОНТЕНТ (ФУНКЦИИ ВОЗВРАЩАЮТ ПОЛНЫЙ HTML)
// =================================================================

// Функция, возвращающая HTML для страницы преподавателя
function getTeacherHtml() {
    // ВНИМАНИЕ: Здесь встроена вся логика Frontend (HTML, CSS, JS)
    return `
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <title>Конструктор Тестов</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f9; }
                .container { max-width: 800px; margin: auto; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }
                h1 { text-align: center; color: #333; }
                input[type="text"], textarea { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
                button { background-color: #007bff; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }
                button:hover { background-color: #0056b3; }
                .question-block { border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; border-radius: 4px; }
                #resultsTable { width: 100%; border-collapse: collapse; margin-top: 20px; }
                #resultsTable th, #resultsTable td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                #resultsTable th { background-color: #f2f2f2; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>✍️ Панель Преподавателя</h1>
                
                <h2>Создать новый тест</h2>
                <input type="text" id="testTitle" placeholder="Название теста">
                <div id="questionsContainer">
                    </div>
                <button onclick="addQuestion()">Добавить вопрос</button>
                <button onclick="createTest()">Получить ссылку</button>
                <p id="linkOutput" style="margin-top: 20px; font-weight: bold;"></p>

                <h2>Просмотреть результаты</h2>
                <input type="text" id="testIdForResults" placeholder="ID теста (из ссылки)">
                <button onclick="viewResults()">Показать результаты</button>

                <table id="resultsTable">
                    <thead>
                        <tr>
                            <th>Имя</th>
                            <th>Группа</th>
                            <th>Баллы</th>
                            <th>Дата</th>
                        </tr>
                    </thead>
                    <tbody>
                        </tbody>
                </table>
            </div>

            <script>
                // Логика Frontend (JavaScript)
                let questionCounter = 0;

                function addQuestion() {
                    questionCounter++;
                    const container = document.getElementById('questionsContainer');
                    const block = document.createElement('div');
                    block.className = 'question-block';
                    block.innerHTML = \`
                        <h4>Вопрос \${questionCounter}</h4>
                        <textarea id="q\${questionCounter}-text" placeholder="Текст вопроса"></textarea>
                        <input type="text" id="q\${questionCounter}-answer" placeholder="Правильный ответ">
                        <input type="number" id="q\${questionCounter}-score" placeholder="Баллы за вопрос" value="1" min="1">
                    \`;
                    container.appendChild(block);
                }

                async function createTest() {
                    const title = document.getElementById('testTitle').value;
                    const questions = [];

                    for (let i = 1; i <= questionCounter; i++) {
                        const text = document.getElementById(\`q\${i}-text\`).value;
                        const answer = document.getElementById(\`q\${i}-answer\`).value;
                        const score = parseInt(document.getElementById(\`q\${i}-score\`).value);
                        
                        if (text && answer && !isNaN(score)) {
                            questions.push({ text, answer: answer.trim().toLowerCase(), score });
                        }
                    }

                    if (!title || questions.length === 0) {
                        alert('Пожалуйста, заполните название и добавьте хотя бы один вопрос.');
                        return;
                    }

                    try {
                        const response = await fetch('/api/tests', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ title, questions })
                        });
                        const data = await response.json();
                        
                        if (response.ok) {
                            document.getElementById('linkOutput').innerHTML = 
                                \`Тест создан! Ссылка: <a href="\${data.link}" target="_blank">\${data.link}</a> (ID: \${data.test_id})\`;
                            // Очистка формы
                            document.getElementById('testTitle').value = '';
                            document.getElementById('questionsContainer').innerHTML = '';
                            questionCounter = 0;
                        } else {
                            alert('Ошибка при создании теста: ' + data.error);
                        }
                    } catch (error) {
                        console.error('Fetch error:', error);
                        alert('Произошла ошибка сети.');
                    }
                }

                async function viewResults() {
                    const testId = document.getElementById('testIdForResults').value;
                    if (!testId) return alert('Введите ID теста.');

                    try {
                        const response = await fetch(\`/api/tests/\${testId}/results\`);
                        const results = await response.json();

                        const tbody = document.querySelector('#resultsTable tbody');
                        tbody.innerHTML = ''; // Очистка старых результатов

                        if (results.length === 0) {
                            tbody.innerHTML = '<tr><td colspan="4">Пока нет результатов.</td></tr>';
                            return;
                        }
                        
                        // Backend уже отсортировал по группе, просто отображаем
                        results.forEach(res => {
                            const row = tbody.insertRow();
                            row.insertCell().textContent = res.student_name;
                            row.insertCell().textContent = res.student_group;
                            row.insertCell().textContent = res.score;
                            row.insertCell().textContent = new Date(res.created_at).toLocaleString('ru-RU');
                        });

                    } catch (error) {
                        console.error('Fetch error:', error);
                        alert('Ошибка при загрузке результатов.');
                    }
                }
                
                // Добавляем один вопрос при загрузке
                window.onload = addQuestion;
            </script>
        </body>
        </html>
    `;
}

// Функция, возвращающая HTML для страницы ученика
function getStudentQuizHtml(testId) {
    return `
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <title>Прохождение Теста</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f9; }
                .container { max-width: 600px; margin: auto; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }
                h1 { text-align: center; color: #333; }
                input[type="text"] { width: 100%; padding: 10px; margin: 5px 0 15px 0; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
                button { background-color: #28a745; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; margin-top: 20px; }
                button:hover { background-color: #1e7e34; }
                .question-item { margin-bottom: 20px; padding: 10px; border: 1px solid #eee; border-radius: 4px; }
                #quizContainer { margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1 id="testTitle">Загрузка теста...</h1>
                
                <p>Введите свои данные:</p>
                <input type="text" id="studentName" placeholder="Фамилия и Имя" required>
                <input type="text" id="studentGroup" placeholder="Номер группы" required>

                <div id="quizContainer">
                    </div>

                <button id="submitBtn" onclick="submitTest()">Завершить тест</button>
                <p id="feedback" style="margin-top: 20px; font-weight: bold;"></p>
            </div>

            <script>
                // Логика Frontend (JavaScript) для ученика
                const testId = \`${testId}\`;
                let questionsData = [];

                async function loadTest() {
                    try {
                        const response = await fetch(\`/api/tests/\${testId}\`);
                        if (!response.ok) {
                            document.getElementById('testTitle').textContent = 'Ошибка: Тест не найден или удален.';
                            document.getElementById('submitBtn').style.display = 'none';
                            return;
                        }
                        const data = await response.json();
                        questionsData = data.questions;
                        document.getElementById('testTitle').textContent = data.title;
                        renderQuestions();
                    } catch (error) {
                        document.getElementById('testTitle').textContent = 'Ошибка загрузки теста.';
                        document.getElementById('submitBtn').style.display = 'none';
                        console.error('Error loading test:', error);
                    }
                }

                function renderQuestions() {
                    const container = document.getElementById('quizContainer');
                    container.innerHTML = '';
                    questionsData.forEach((q, index) => {
                        const block = document.createElement('div');
                        block.className = 'question-item';
                        block.innerHTML = \`
                            <p><strong>\${index + 1}. \${q.text}</strong> (\${q.score} балл.)</p>
                            <input type="text" id="ans-\${index}" class="student-answer" placeholder="Ваш ответ">
                        \`;
                        container.appendChild(block);
                    });
                }

                async function submitTest() {
                    const studentName = document.getElementById('studentName').value.trim();
                    const studentGroup = document.getElementById('studentGroup').value.trim();
                    
                    if (!studentName || !studentGroup) {
                        alert('Пожалуйста, введите свое имя и группу.');
                        return;
                    }

                    let totalScore = 0;

                    questionsData.forEach((q, index) => {
                        const studentAnswerInput = document.getElementById(\`ans-\${index}\`);
                        const studentAnswer = studentAnswerInput ? studentAnswerInput.value.trim().toLowerCase() : '';
                        
                        // Сравнение ответа с правильным (нечувствительно к регистру и пробелам по краям)
                        if (studentAnswer === q.answer) {
                            totalScore += q.score;
                        }
                    });

                    // Отправка результатов на сервер
                    try {
                        const response = await fetch('/api/results', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                test_id: parseInt(testId),
                                student_name: studentName,
                                student_group: studentGroup,
                                score: totalScore
                            })
                        });

                        if (response.ok) {
                            document.getElementById('quizContainer').innerHTML = '';
                            document.getElementById('submitBtn').style.display = 'none';
                            document.getElementById('feedback').style.color = 'green';
                            document.getElementById('feedback').textContent = 
                                \`Тест успешно завершен! Ваш результат: \${totalScore} баллов.\`;
                        } else {
                            document.getElementById('feedback').style.color = 'red';
                            document.getElementById('feedback').textContent = 'Ошибка при сохранении результатов. Попробуйте снова.';
                        }
                    } catch (error) {
                        console.error('Submit error:', error);
                        document.getElementById('feedback').style.color = 'red';
                        document.getElementById('feedback').textContent = 'Произошла ошибка сети.';
                    }
                }

                loadTest();
            </script>
        </body>
        </html>
    `;
}
