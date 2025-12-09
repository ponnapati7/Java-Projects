📌 AI Startup Task Manager
AI Startup Task Manager is a simple Java console application used to manage employees and their tasks inside a startup. The program allows creating employees, adding tasks with priority levels and due dates, assigning each task to an employee, checking which tasks are overdue, and displaying all tasks in different views. The application runs fully in the console and does not use any database. All data is stored in memory during the program execution.
The program shows how tasks can be filtered, grouped by employees, and exported to a text file. It also runs a small background thread that prints task reports while the main program runs. This gives a quick demonstration of how a basic task management system works using pure Java.

✨ Features
Add employees to the system
Create new tasks with title, priority and due date
Assign tasks to employees
View all tasks
Check overdue tasks
Filter tasks based on priority
Group tasks by employee
Export all task details to a text file
Background reporting using a separate thread

🛠️ Tools & Technologies
Java 17
IntelliJ IDEA
Maven

📂 Project Structure
src/main/java/com/ai/ocp/project/AiStartupTaskManager.java
All the classes like Employee, Task, Repository, and TaskService are written inside the same file to keep the project simple and easy to run.

🚀 How to Run the Program
Install JDK 17
Open the project in IntelliJ IDEA
Go to:
src/main/java/com/ai/ocp/project/AiStartupTaskManager.java
Right-click the file and click Run
Output will be shown in the console
A file named tasks_export.txt will be created with task data

📄 Output Information
The console output includes:
List of employees
List of tasks
High priority tasks
Tasks due soon
Overdue tasks
Tasks grouped by employee
Background report messages

👨‍💻 Author
Gayathri Ponnapati
