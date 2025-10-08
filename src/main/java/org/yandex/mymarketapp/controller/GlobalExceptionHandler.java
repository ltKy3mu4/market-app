package org.yandex.mymarketapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.yandex.mymarketapp.model.exception.CartPositionFoundException;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.exception.MarketExcpetion;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {CartPositionFoundException.class, ItemNotFoundException.class, OrderNotFoundException.class})
    public ResponseEntity<ModelAndView> handleNotFoundException(RuntimeException e) {
        log.error("Error occurred: {}", e.getMessage(), e);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error/404");
        modelAndView.addObject("errorMessage", e.getMessage());
        modelAndView.addObject("errorCode", 404);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(modelAndView);
    }
}
