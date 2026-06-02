package com.sgitu.g4.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class G3NotificationRecipientsPage {

	private List<G3NotificationRecipientItem> items = new ArrayList<>();
	private int page;
	private int size;
	private long total;
}
