package org.yakdanol.notificationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_recipients")
public class EmailRecipient implements Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Override
    public String getContactInfo() {
        return email;
    }
}
