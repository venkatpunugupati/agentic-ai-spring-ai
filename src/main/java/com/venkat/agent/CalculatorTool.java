package com.venkat.agent;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component 
public class CalculatorTool {
    @Tool(description ="Evaluate an arithmetic expression (supports + - * / % ** and parentheses) and return the result")
    public String calculate(@ToolParam(description ="the arithmetic expression, e.g. \"(2+3)*4\"") String expression) {
        System.out.println("[TOOL] calculate called with: "+ expression);
        if (expression == null || !expression.matches("[\\d\\s+\\-*/%().]+")) {
            return "Error: invalid characters in expression";
        }
        try {
            return String.valueOf(new Parser(expression).parse());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static class Parser {
        private final String s;
        private int pos;

        Parser(String s) { this.s = s; }

        double parse() {
            double v = expr();
            skip();
            if (pos != s.length()) throw new IllegalArgumentException("unexpected trailing characters");
            return v;
        }

        private double expr() {
            double v = term();
            while (true) {
                skip();
                if (match('+')) v += term();
                else if (match('-')) v -= term();
                else return v;
            }
        }

        private double term() {
            double v = power();
            while (true) {
                skip();
                if (match('*')) v *= power();
                else if (match('/')) v /= power();
                else if (match('%')) v %= power();
                else return v;
            }
        }

        private double power() {
            double v = factor();
            skip();
            if (pos + 1 < s.length() && s.charAt(pos) == '*' && s.charAt(pos + 1) == '*') {
                pos += 2;
                v = Math.pow(v, power());
            }
            return v;
        }

        private double factor() {
            skip();
            if (match('(')) {
                double v = expr();
                skip();
                if (!match(')')) throw new IllegalArgumentException("missing ')'");
                return v;
            }
            if (peek() == '-' || peek() == '+') {
                return match('-') ? -factor() : factor();
            }
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (start == pos) throw new IllegalArgumentException("expected a number");
            return Double.parseDouble(s.substring(start, pos));
        }

        private void skip() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private char peek() {
            skip();
            return pos < s.length() ? s.charAt(pos) : 0;
        }

        private boolean match(char c) {
            skip();
            if (pos < s.length() && s.charAt(pos) == c) { pos++; return true; }
            return false;
        }
    }
}
