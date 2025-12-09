package com.ai.ocp.project;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/* ---- domain enums ---- */

enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

enum Priority {
    LOW,
    MEDIUM,
    HIGH
}

/* ---- Base identifiable interface ---- */

interface Identifiable {
    long getId();
}

/* ---- Person abstraction ---- */

abstract class Person implements Comparable<Person>, Identifiable {
    private final long id;
    private String name;
    private String email;

    protected Person(long id, String name, String email) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
    }

    protected Person(long id, String name) {
        this(id, name, name.toLowerCase().replace(" ", ".") + "@gmail.com");
    }

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract String getRoleDescription();

    @Override
    public int compareTo(Person other) {
        return this.name.compareToIgnoreCase(other.name);
    }

    @Override
    public String toString() {
        return "Person{id=%d, name='%s', email='%s', role='%s'}"
                .formatted(id, name, email, getRoleDescription());
    }
}

/* ---- Employee implementation ---- */

class Employee extends Person {

    private int experienceYears;

    public Employee(long id, String name, String email, int experienceYears) {
        super(id, name, email);
        this.experienceYears = experienceYears;
    }

    public Employee(long id, String name, int experienceYears) {
        super(id, name);
        this.experienceYears = experienceYears;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    @Override
    public String getRoleDescription() {
        return "Employee with %d years experience".formatted(experienceYears);
    }
}

/* ---- Functional filter ---- */

@FunctionalInterface
interface TaskFilter {
    boolean test(Task task);

    default TaskFilter and(TaskFilter other) {
        return t -> this.test(t) && other.test(t);
    }

    static TaskFilter highPriority() {
        return t -> t.getPriority() == Priority.HIGH;
    }
}

/* ---- Task entity ---- */

class Task implements Identifiable {

    private final long id;
    private String title;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private Employee assignedTo;

    private Task(long id, String title, TaskStatus status,
                 Priority priority, LocalDate dueDate, Employee assignedTo) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.status = Objects.requireNonNull(status);
        this.priority = Objects.requireNonNull(priority);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.assignedTo = assignedTo;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public long getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }
    public void setStatus(TaskStatus status) {
        this.status = Objects.requireNonNull(status);
    }


    public Priority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Optional<Employee> getAssignedTo() {
        return Optional.ofNullable(assignedTo);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) && status != TaskStatus.DONE;
    }

    @Override
    public String toString() {
        String assignedName = (assignedTo != null) ? assignedTo.getName() : "unassigned";
        return "Task{id=%d, title='%s', status=%s, priority=%s, dueDate=%s, assignedTo=%s}"
                .formatted(id, title, status, priority, dueDate, assignedName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    /* ---- Builder ---- */
    public static class Builder {
        private long id;
        private String title;
        private TaskStatus status = TaskStatus.TODO;
        private Priority priority = Priority.MEDIUM;
        private LocalDate dueDate = LocalDate.now().plusDays(7);
        private Employee assignedTo;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder assignedTo(Employee assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public Task build() {
            return new Task(id, title, status, priority, dueDate, assignedTo);
        }
    }
}

/* ---- Generic Repository ---- */

interface Repository<T extends Identifiable> {

    T save(T entity);

    Optional<T> findById(long id);

    List<T> findAll();

    void delete(long id);

    default List<T> findAllSorted(Comparator<? super T> comparator) {
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }
}

/* ---- In-memory Repository ---- */

class InMemoryRepository<T extends Identifiable> implements Repository<T> {

    private final Map<Long, T> storage = new HashMap<>();

    @Override
    public T save(T entity) {
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void delete(long id) {
        storage.remove(id);
    }
}

/* ---- Custom Exception ---- */

class TaskNotFoundException extends Exception {
    public TaskNotFoundException(long id) {
        super("Task with id %d not found".formatted(id));
    }
}

/* ---- Service Layer ---- */

class TaskService {

    private final Repository<Task> repo;

    public TaskService(Repository<Task> repo) {
        this.repo = repo;
    }

    public Task create(Task task) {
        return repo.save(task);
    }

    public Task updateStatus(long id, TaskStatus status) throws TaskNotFoundException {
        Task task = repo.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(status);
        return repo.save(task);
    }

    public List<Task> findTasks(TaskFilter filter) {
        return repo.findAll().stream().filter(filter::test).collect(Collectors.toList());
    }

    public List<Task> dueIn(int days) {
        LocalDate now = LocalDate.now();
        LocalDate limit = now.plusDays(days);
        return repo.findAll().stream()
                .filter(t -> !t.getDueDate().isBefore(now) && !t.getDueDate().isAfter(limit))
                .collect(Collectors.toList());
    }

    public Map<Employee, List<Task>> byEmployee() {
        return repo.findAll().stream()
                .filter(t -> t.getAssignedTo().isPresent())
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().get()));
    }

    public Set<Task> overdue() {
        return repo.findAll().stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toCollection(HashSet::new));
    }
}

/* ---- File Export ---- */

class TaskFileExporter implements AutoCloseable {

    private final BufferedWriter writer;

    public TaskFileExporter(Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path);
    }

    public void write(Task task) {
        try {
            writer.write(task.toString());
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

/* ---- ID Generator ---- */

class IdGenerator {

    private static final AtomicLong TASK = new AtomicLong(1);
    private static final AtomicLong EMP = new AtomicLong(1);

    public static long nextTask() {
        return TASK.getAndIncrement();
    }

    public static long nextEmployee() {
        return EMP.getAndIncrement();
    }
}

/* ---- Background Thread Reporter ---- */

class BackgroundReporter implements Runnable {

    private final TaskService service;
    private boolean running = true;

    public BackgroundReporter(TaskService service) {
        this.service = service;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Simple Stats
                System.out.println("[Report] Total tasks: " + service.findTasks(t -> true).size());
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}

/* ---- Main Application ---- */

public class AiStartupTaskManager {

    public static void main(String[] args) {

        Repository<Employee> employees = new InMemoryRepository<>();
        Repository<Task> tasks = new InMemoryRepository<>();
        TaskService service = new TaskService(tasks);

        Employee e1 = new Employee(IdGenerator.nextEmployee(), "Siri Reddy", "siri@aistartup.com", 3);
        Employee e2 = new Employee(IdGenerator.nextEmployee(), "Suraj Kumar", 5);

        employees.save(e1);
        employees.save(e2);

        System.out.println("--- Employees ---");
        employees.findAllSorted(Comparator.naturalOrder()).forEach(System.out::println);

        Period diff = Period.ofYears(Math.abs(e1.getExperienceYears() - e2.getExperienceYears()));
        System.out.println("Experience diff: " + diff.getYears() + " years\n");

        Task t1 = Task.builder()
                .id(IdGenerator.nextTask())
                .title("Build first MVP model")
                .status(TaskStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .assignedTo(e1)
                .build();

        Task t2 = Task.builder()
                .id(IdGenerator.nextTask())
                .title("Set up CI/CD pipeline")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(5))
                .assignedTo(e2)
                .build();

        Task t3 = Task.builder()
                .id(IdGenerator.nextTask())
                .title("Prepare pitch deck")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().minusDays(1))
                .assignedTo(e1)
                .build();

        service.create(t1);
        service.create(t2);
        service.create(t3);

        System.out.println("--- All Tasks ---");
        tasks.findAll().forEach(System.out::println);

        BackgroundReporter reporter = new BackgroundReporter(service);
        Thread thread = new Thread(reporter);
        thread.setDaemon(true);
        thread.start();

        System.out.println("\n--- High Priority Not Done ---");
        service.findTasks(TaskFilter.highPriority().and(t -> t.getStatus() != TaskStatus.DONE))
                .forEach(System.out::println);

        System.out.println("\n--- Due in 3 days ---");
        service.dueIn(3).forEach(System.out::println);

        System.out.println("\n--- Overdue ---");
        service.overdue().forEach(System.out::println);

        System.out.println("\n--- By Employee ---");
        service.byEmployee().forEach((emp, list) ->
                System.out.println(emp.getName() + " -> " + list));

        Path export = Path.of("tasks_export.txt");
        try (TaskFileExporter exp = new TaskFileExporter(export)) {
            for (Task t : tasks.findAll()) exp.write(t);
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }

        try {
            Thread.sleep(4000);
        } catch (InterruptedException ignored) { }

        reporter.stop();
        System.out.println("\nFinished.");
    }
}
