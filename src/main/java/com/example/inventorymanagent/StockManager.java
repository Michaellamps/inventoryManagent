package com.example.inventorymanagent;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;

public class StockManager extends Application {
    private final Connection connection;
    private final Map<String, Integer> inventory;
    private final Map<String, String> vendors;
    private final ObservableList<String> goodsList;
    private ListView<String> goodsListView;

    public StockManager() {
        inventory = new HashMap<>();
        vendors = new HashMap<>();
        goodsList = FXCollections.observableArrayList();
        connection = establishDatabaseConnection();
        createTables();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Inventory Management System");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        // Category selection
        Label categoryLabel = new Label("Select Category:");
        ComboBox<String> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(
                "Beverages",
                "Bread/Bakery",
                "Canned/Jarred Goods",
                "Dairy Products",
                "Dry/Baking Goods",
                "Frozen Products",
                "Meat",
                "Farm Produce",
                "Home Cleaners",
                "Paper Goods",
                "Home Care"
        );
        grid.add(categoryLabel, 0, 0);
        grid.add(categoryComboBox, 1, 0);

        // Item input
        Label itemLabel = new Label("Item Name:");
        TextField itemTextField = new TextField();
        grid.add(itemLabel, 0, 1);
        grid.add(itemTextField, 1, 1);

        // Quantity input
        Label quantityLabel = new Label("Quantity:");
        TextField quantityTextField = new TextField();
        grid.add(quantityLabel, 0, 2);
        grid.add(quantityTextField, 1, 2);

        // Add item button
        Button addItemButton = new Button("Add Item");
        addItemButton.setOnAction(e -> {
            String category = categoryComboBox.getValue();
            String item = itemTextField.getText();
            int quantity = Integer.parseInt(quantityTextField.getText());

            addItem(category, item, quantity);
            updateGoodsListView();
        });
        grid.add(addItemButton, 1, 3);

        // Goods list view
        goodsListView = new ListView<>(goodsList);
        goodsListView.setPrefHeight(200);
        grid.add(goodsListView, 2, 0, 1, 4);

        // Vendor input
        Label vendorLabel = new Label("Vendor:");
        TextField vendorTextField = new TextField();
        grid.add(vendorLabel, 0, 4);
        grid.add(vendorTextField, 1, 4);

        // Product code input
        Label productCodeLabel = new Label("Product Code:");
        TextField productCodeTextField = new TextField();
        grid.add(productCodeLabel, 0, 5);
        grid.add(productCodeTextField, 1, 5);

        // Add vendor button
        Button addVendorButton = new Button("Add Vendor");
        addVendorButton.setOnAction(e -> {
            String vendor = vendorTextField.getText();
            String productCode = productCodeTextField.getText();

            addVendor(vendor, productCode);
        });
        grid.add(addVendorButton, 1, 6);

        // View vendors button
        Button viewVendorsButton = new Button("View Vendors");
        viewVendorsButton.setOnAction(e -> viewVendors());
        grid.add(viewVendorsButton, 2, 4);

        // View goods button
        Button viewGoodsButton = new Button("View Goods");
        viewGoodsButton.setOnAction(e -> {
            String category = categoryComboBox.getValue();
            viewGoods(category);
        });
        grid.add(viewGoodsButton, 2, 5);

        // View issued goods button
        Button viewIssuedGoodsButton = new Button("View Issued Goods");
        viewIssuedGoodsButton.setOnAction(e -> viewIssuedGoods());
        grid.add(viewIssuedGoodsButton, 2, 6);

        // Issue goods input
        Label issueGoodsLabel = new Label("Issue Goods:");
        TextField issueGoodsTextField = new TextField();
        grid.add(issueGoodsLabel, 0, 7);
        grid.add(issueGoodsTextField, 1, 7);

        // Issue goods button
        Button issueGoodsButton = new Button("Issue Goods");
        issueGoodsButton.setOnAction(e -> {
            String item = issueGoodsTextField.getText();
            int quantity = Integer.parseInt(quantityTextField.getText());

            issueGoods(item, quantity);
            updateGoodsListView();
        });
        grid.add(issueGoodsButton, 1, 8);

        // View bills button
        Button viewBillsButton = new Button("View Bills");
        viewBillsButton.setOnAction(e -> viewBills());
        grid.add(viewBillsButton, 2, 7);

        Scene scene = new Scene(grid, 700, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Establish database connection
    private Connection establishDatabaseConnection() {
        Connection conn = null;
        try {
            String url = "jdbc:mysql://localhost:3306/inventorydb";
            String username = "your-username";
            String password = "your-password";
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to the database!");
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database.");
            e.printStackTrace();
        }
        return conn;
    }

    // Create necessary tables if they don't exist
    private void createTables() {
        try {
            Statement statement = connection.createStatement();
            String createInventoryTable = "CREATE TABLE IF NOT EXISTS inventory (" +
                    "item VARCHAR(255) PRIMARY KEY," +
                    "quantity INT NOT NULL)";
            statement.executeUpdate(createInventoryTable);

            String createVendorsTable = "CREATE TABLE IF NOT EXISTS vendors (" +
                    "productCode VARCHAR(255) PRIMARY KEY," +
                    "vendor VARCHAR(255) NOT NULL)";
            statement.executeUpdate(createVendorsTable);

            statement.close();
        } catch (SQLException e) {
            System.out.println("Failed to create database tables.");
            e.printStackTrace();
        }
    }

    // Add a new item to the inventory
    private void addItem(String category, String item, int quantity) {
        inventory.put(item, quantity);

        switch (category) {
            case "Beverages":
            case "Bread/Bakery":
            case "Canned/Jarred Goods":
            case "Dairy Products":
                goodsList.add(item);
                break;
            default:
                System.out.println("Invalid category.");
        }

        // Update the database
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO inventory VALUES (?, ?)");
            statement.setString(1, item);
            statement.setInt(2, quantity);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            System.out.println("Failed to update the database.");
            e.printStackTrace();
        }

        // Balance the stock
        balanceStock();
    }

    // Add a vendor to the list of vendors
    private void addVendor(String vendor, String productCode) {
        vendors.put(productCode, vendor);

        // Update the database
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO vendors VALUES (?, ?)");
            statement.setString(1, productCode);
            statement.setString(2, vendor);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            System.out.println("Failed to update the database.");
            e.printStackTrace();
        }
    }

    // View the list of vendors
    private void viewVendors() {
        for (Map.Entry<String, String> entry : vendors.entrySet()) {
            System.out.println("Product Code: " + entry.getKey() + ", Vendor: " + entry.getValue());
        }
    }

    // View the goods in a specific category
    private void viewGoods(String category) {
        switch (category) {
            case "Beverages":
            case "Bread/Bakery":
            case "Canned/Jarred Goods":
            case "Dairy Products":
                for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                    if (goodsList.contains(entry.getKey())) {
                        System.out.println("Item: " + entry.getKey() + ", Quantity: " + entry.getValue());
                    }
                }
                break;
            default:
                System.out.println("Invalid category.");
        }
    }

    // View the list of issued goods
    private void viewIssuedGoods() {
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            if (!goodsList.contains(entry.getKey())) {
                System.out.println("Item: " + entry.getKey() + ", Quantity: " + entry.getValue());
            }
        }
    }

    // Issue goods by removing them from the inventory
    private void issueGoods(String item, int quantity) {
        if (inventory.containsKey(item)) {
            int currentQuantity = inventory.get(item);
            if (currentQuantity >= quantity) {
                inventory.put(item, currentQuantity - quantity);

                // Update the database
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE inventory SET quantity = ? WHERE item = ?");
                    statement.setInt(1, currentQuantity - quantity);
                    statement.setString(2, item);
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    System.out.println("Failed to update the database.");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Insufficient quantity in inventory.");
            }
        } else {
            System.out.println("Item not found in inventory.");
        }
    }

    // View bills for sold goods (sales file)
    private void viewBills() {
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            if (!goodsList.contains(entry.getKey())) {
                System.out.println("Item: " + entry.getKey() + ", Quantity: " + entry.getValue());
            }
        }
    }

    // Balance the stock to avoid too high or too low levels
    private void balanceStock() {
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String item = entry.getKey();
            int quantity = entry.getValue();

            if (quantity > 100) {
                System.out.println("Item: " + item + " - Stock level too high. Quantity reduced to 100.");
                inventory.put(item, 100);

                // Update the database
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE inventory SET quantity = ? WHERE item = ?");
                    statement.setInt(1, 100);
                    statement.setString(2, item);
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    System.out.println("Failed to update the database.");
                    e.printStackTrace();
                }
            } else if (quantity < 10) {
                System.out.println("Item: " + item + " - Stock level too low. Quantity increased to 10.");
                inventory.put(item, 10);

                // Update the database
                try {
                    PreparedStatement statement = connection.prepareStatement("UPDATE inventory SET quantity = ? WHERE item = ?");
                    statement.setInt(1, 10);
                    statement.setString(2, item);
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    System.out.println("Failed to update the database.");
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateGoodsListView() {
        goodsListView.setItems(FXCollections.observableArrayList(goodsList));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
