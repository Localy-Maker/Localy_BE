package org.example.localy.dto.admin;


import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateAnnouncementRequest {
    private String title;
    private String content;
}
