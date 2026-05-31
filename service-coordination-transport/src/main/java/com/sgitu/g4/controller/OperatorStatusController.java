package com.sgitu.g4.controller;

import com.sgitu.g4.dto.OperatorStatusResponse;
import com.sgitu.g4.service.SupervisionAggregateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Supervision opérateur")
@RestController
@RequestMapping("/api/v1/operator")
@RequiredArgsConstructor
public class OperatorStatusController {

	private final SupervisionAggregateService supervisionAggregateService;

	@GetMapping("/status")
	public OperatorStatusResponse status() {
		return supervisionAggregateService.operatorStatus();
	}
}
