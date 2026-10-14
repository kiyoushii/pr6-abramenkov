# ПР №6. Небольшая логическая задача с ветвлениями и конечным набором решений

Абраменков Андрей, группа ЭФБО-12-25. Вариант 1.

Программа направляет служебную заявку на один из пяти маршрутов согласования по четырём параметрам
(сумма, срочность, уровень риска, наличие персональных данных). По варианту 1 порог финансового
контроля равен 250 000, а метод `addRiskScore` считает числовой риск-скор.

## Сборка и запуск

Требуется Java SE 21 или выше (`java -version`).

```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr03.SelfCheck
```

Windows: запустить `run_selfcheck.bat` (включает UTF-8, собирает и запускает проект).

`SelfCheck` выполняет unit-проверки, прогоняет `data/sample.csv`, затем включает интерактивный режим:
вводите заявку в формате `amount;urgent;risk;personal`, выход по `exit` или пустой строке.
Пакет называется `ru.edu.pr03` по требованию методички.

## Структура

- `src/ru/edu/pr03/ApprovalRouter.java` — метод `route` и метод варианта `addRiskScore`
- `src/ru/edu/pr03/ApprovalRoute.java`, `RiskLevel.java` — enum
- `src/ru/edu/pr03/SelfCheck.java` — точка входа: unit-проверки, data-driven, интерактив
- `data/sample.csv` — контрольные значения
- `diagrams/` — `activity.dot/png`, `class.dot/png`
- `DECISIONS.md` — решения и ответы на вопросы
- `REPORT.docx` — отчёт
