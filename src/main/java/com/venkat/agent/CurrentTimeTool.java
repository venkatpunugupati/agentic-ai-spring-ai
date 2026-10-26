package com.venkat.agent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
@Component 
public class CurrentTimeTool {

    @Tool(description="Returns the current local date and time. Call this for any question about the current time or date.")
    public String getCurrentTime() {
        System.out.println("[TOOL] getCurrentTime called with: ");
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
