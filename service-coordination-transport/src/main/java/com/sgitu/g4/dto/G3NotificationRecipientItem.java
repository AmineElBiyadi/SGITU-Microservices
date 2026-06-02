package com.sgitu.g4.dto;

import lombok.Data;

@Data
public class G3NotificationRecipientItem {

	/** G3 renvoie un identifiant numérique (ex. 1, 2). */
	private Long userId;
	private String email;
}
