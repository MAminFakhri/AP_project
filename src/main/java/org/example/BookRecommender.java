package org.example;

import java.io.*;
import java.util.*;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import org.neo4j.driver.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BookRecommender {

    private static final String CSV_FILE = "books.csv";
    private static final Driver neo4jDriver = GraphDatabase.driver("bolt://localhost:7687",
            AuthTokens.basic("neo4j", "Ma1371384#"));

    public static void main(String[] args) {
        List<Book> books = readCSV();
        storeInNeo4j(books);

        String bookName = "Harry Potter";

        // Get recommendations from Neo4j
        List<String> neo4jRecommendations = getSimilarBooks(bookName);
        System.out.println("Neo4j Recommended Books: " + neo4jRecommendations);

        // Get recommendations from LLaMA API
        List<String> llamaRecommendations = callLlamaAPI(bookName);
        System.out.println("LLaMA API Recommended Books: " + llamaRecommendations);
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
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
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

    public static List<String> callLlamaAPI(String bookName) {
        List<String> recommendations = new ArrayList<>();

        String apiUrl = "";

        // JSON payload
        String jsonPayload = "{\"book\": \"" + bookName + "\"}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(responseBody);

            for (JsonNode bookNode : jsonResponse.get("recommendations")) {
                recommendations.add(bookNode.asText());
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return recommendations;
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
