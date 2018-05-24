/*
 * Copyright 2018 Xavier Sumba <xavier.sumba93@ucuenca.ec>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ec.edu.cedia.redi.latindex.api;

import ec.edu.cedia.redi.latindex.utils.ModifiedJaccard;
import ec.edu.cedia.redi.latindex.search.query.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public abstract class Query {

    private static final int MAX_WORDS = 32;
    private static final int MAX_CHARACTERS = 233;
    protected boolean cutWords = true;

    public String buildQueryWords() {
        String query = "";
        int maxwords = MAX_WORDS;
        List<Value> parameters = new ArrayList<>(getParameters());
        List<Value> exact_parameters = parameters.stream()
                .filter(v -> v.getFrequency() == -1)
                .collect(Collectors.toList());

        // Add complete words.
        for (Value v : exact_parameters) {
            maxwords -= countWords(v.getValue());
            query += String.format(template(), v.getValue());
            parameters.remove(v);
        }

        // Stop if there are more than 16 complete words. 
        if (maxwords < (int) MAX_WORDS * .5) {
            // stop
        }

        // Build query with parameters according to frequencies.
        for (Value v : parameters) {
            int n = maxwords * v.getFrequency() / 100;
            String chopWords = String.format(template(), getNWords(v.getValue(), n));

            query += chopWords;
            maxwords -= countWords(chopWords);
        }

        // Throw exception if an overflow happened.
        if (maxwords < 0) {
            // stop
        }
        // TODO: Improve residual words.
        return query;
    }

    public String buildQueryCharacters() {
        String query = "";
        int maxcharacters = MAX_CHARACTERS;
        List<Value> parameters = new ArrayList<>(getParameters());
        List<Value> exact_parameters = parameters.stream()
                .filter(v -> v.getFrequency() == -1)
                .collect(Collectors.toList());

        // Add complete words.
        for (Value v : exact_parameters) {
            String value = String.format(template(), v.getValue());
            maxcharacters -= countCharacters(value);
            query += value;
            parameters.remove(v);
        }

        // Stop if there are more than 16 complete words. 
        if (maxcharacters < (int) MAX_CHARACTERS * .5) {
            // stop
        }

        // Build query with parameters according to frequencies.
        for (int i = 0; i < parameters.size(); i++) {
            Value v = parameters.get(i);
            int n = i == parameters.size() - 1 ? maxcharacters : maxcharacters * v.getFrequency() / 100;
            n -= extraCharactersInTemplate();
            String chopWords = String.format(template(), getNCharactersWords(v.getValue(), n));

            query += chopWords;
            maxcharacters -= countCharacters(chopWords);
        }

        // Throw exception if an overflow happened.
        if (maxcharacters < 0) {
            // stop
        }

        return query;
    }

    protected abstract List<Value> getParameters();

    protected abstract String template();

    protected abstract int extraCharactersInTemplate();

    private int countCharacters(String s) {
        return s.length();
    }

    private int countWords(String s) {
        ModifiedJaccard mod = new ModifiedJaccard();
        return mod.tokenizer(s).size();
    }

    private String getNCharactersWords(String s, int n) {
        ModifiedJaccard mod = new ModifiedJaccard();
        List<String> words = mod.tokenizer(s);
        int num_words = 0;
        for (String w : words) {
            if (n >= w.length() + 1) {
                num_words++;
                n -= w.length() + 1;
            } else {
                break;
            }
        }
        return getNWords(s, num_words);
    }

    private String getNWords(String s, int n) {
        ModifiedJaccard mod = new ModifiedJaccard();
        List<String> tokenizer = mod.tokenizer(s);
        List<String> subList = tokenizer.subList(0, n < tokenizer.size() ? n : tokenizer.size());
        return String.join(" ", subList);
    }
}
