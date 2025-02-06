package bookrecom;

package mypackage;

public class BookRecommender {
    public static void main(String[] args) {
        // اتصال به دیتابیس Neo4j
        Neo4jConnection connection = new Neo4jConnection();
        connection.connect();

        // اضافه کردن داده‌ها به دیتابیس
        Book book1 = new Book("1984", "George Orwell");
        Book book2 = new Book("Brave New World", "Aldous Huxley");
        Book book3 = new Book("Fahrenheit 451", "Ray Bradbury");

        connection.addBook(book1);
        connection.addBook(book2);
        connection.addBook(book3);

        // جستجوی کتاب‌ها و ارائه پیشنهادات
        String userInput = "1984";
        connection.recommendBooks(userInput);

        connection.close();
    }
}
