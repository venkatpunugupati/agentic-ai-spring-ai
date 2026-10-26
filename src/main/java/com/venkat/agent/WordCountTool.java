package com.venkat.agent;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
@Component 
public class WordCountTool {

    @Tool(description="Count the words in a text and return the count")
    public String countWords(@ToolParam(description="the text to count words in") String text) {
        System.out.println("[TOOL] countWords called with: "+ text);
        if (text == null || text.isBlank()) return "0";
        String word_count = String.valueOf(text.trim().split("\\s+").length);
        System.out.println("Word Cont is --> "+ word_count);
        
        return word_count ;
    }
}
