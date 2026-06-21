package com.loanflow.ai.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DocumentTextExtractor {

    private final Tika tika = new Tika();

    public String extract(InputStream inputStream) {
        try {
            return tika.parseToString(inputStream);
        } catch (Exception e) {
            throw new DocumentProcessingException("Unable to extract document text", e);
        }
    }
}
