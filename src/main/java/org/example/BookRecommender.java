package org.example;

import java.io.*;
import java.util.*;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import org.neo4j.driver.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BookRecommender {
    private static final String CSV_FILE = "books.csv";
    private static final Driver neo4jDriver = GraphDatabase.driver("bolt://localhost:7687",
            AuthTokens.basic("neo4j", "password"));

    public static void main(String[] args) {
        List<Book> books = readCSV();
        storeInNeo4j(books);
        String bookName = "Harry Potter";
        List<String> recommendations = getSimilarBooks(bookName);
        System.out.println("Recommended Books: " + recommendations);
    }

    public static List<Book> readCSV() {
        List<Book> books = new ArrayList<>();
        try (Reader reader = new FileReader(CSV_FILE);
             CSVReader csvReader = new CSVReader(reader)) {
            String[] line;
            csvReader.readNext(); // Skip header
            while ((line = csvReader.readNext()) != null) {
                books.add(new Book(line[1], line[2])); // Title, Author
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return books;
    }

    public static void storeInNeo4j(List<Book> books) {
        try (Session session = neo4jDriver.session()) {
            for (Book book : books) {
                session.run("MERGE (b:Book {title: $title, author: $author})",
                        Values.parameters("title", book.title, "author", book.author));
            }
        }
    }

    public static List<String> getSimilarBooks(String bookName) {
        List<String> recommendations = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (b:Book {title: $title})-[:SIMILAR_TO]-(rec:Book) RETURN rec.title",
                    Values.parameters("title", bookName));
            result.stream().forEach(record -> recommendations.add(record.get("rec.title").asString()));
        }
        return recommendations;
    }

    public static String callLlamaAPI(String bookName) {
        // Mock API call
        return "Similar Book 1, Similar Book 2";
    }
}

class Book {
    String title;
    String author;

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
