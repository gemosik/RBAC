package com.example.taxi.user.service;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
