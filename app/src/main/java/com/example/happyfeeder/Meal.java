package com.example.happyfeeder;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meals")
public class Meal {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String quantity;
    private String time;
    private String username;  // Câmpul pentru username este aici

    // Constructor implicit necesar pentru Room
    public Meal() {}

    // Constructor cu parametri, inclusiv username
    public Meal(String name, String quantity, String time, String username) {
        this.name = name;
        this.quantity = quantity;
        this.time = time;
        this.username = username;  // Setează username-ul
    }

    // Getter și Setter pentru id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter și Setter pentru name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter și Setter pentru quantity
    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    // Getter și Setter pentru time
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    // Getter și Setter pentru username
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Meal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", quantity='" + quantity + '\'' +
                ", time='" + time + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
