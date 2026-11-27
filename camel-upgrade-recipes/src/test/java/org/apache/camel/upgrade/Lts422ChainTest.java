/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.upgrade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the 4.22 LTS chain ({@code 4.22LTS.yaml}): every 4.22.x patch recipe is reachable from
 * {@code Camel422LTSMigrationRecipe}, as are the older-line patch recipes the 4.22.x chains reference, and no 4.23
 * recipe is, because the LTS line does not have the 4.23 changes.
 */
public class Lts422ChainTest {

    private static final Path REWRITE_PATH = Paths.get("src/main/resources/META-INF/rewrite");
    private static final String PREFIX = "org.apache.camel.upgrade.";
    private static final String LTS_RECIPE = PREFIX + "Camel422LTSMigrationRecipe";
    private static final Pattern NAME = Pattern.compile("^name:\\s*(\\S+)\\s*$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*-\\s*(org\\.apache\\.camel\\.upgrade\\.[\\w.]+):?\\s*$");
    private static final Pattern PATCH_FILE = Pattern.compile("4\\.22\\.(\\d+)\\.yaml");

    /** Recipes referenced by the 4.22.x patch chains that live in an older patch namespace. */
    private static final List<String> INHERITED = List.of(PREFIX + "camel418_6.upgradeOpenstackSwiftHeaders");

    @Test
    void ltsChainHasEveryPatchRecipeAndNo423Recipe() throws IOException {
        Map<String, List<String>> recipeLists = new HashMap<>();
        List<String> patchRecipes = new ArrayList<>();
        try (Stream<Path> paths = Files.list(REWRITE_PATH)) {
            for (Path yamlFile : paths.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
                Matcher patch = PATCH_FILE.matcher(yamlFile.getFileName().toString());
                if (patch.matches()) {
                    patchRecipes.add(PREFIX + "camel422_" + patch.group(1) + ".CamelMigrationRecipe");
                }
                String current = null;
                for (String line : Files.readAllLines(yamlFile)) {
                    Matcher name = NAME.matcher(line);
                    Matcher item = LIST_ITEM.matcher(line);
                    if (name.matches()) {
                        current = name.group(1);
                        recipeLists.putIfAbsent(current, new ArrayList<>());
                    } else if (current != null && item.matches()) {
                        recipeLists.get(current).add(item.group(1));
                    }
                }
            }
        }

        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> todo = new ArrayDeque<>(List.of(LTS_RECIPE));
        while (!todo.isEmpty()) {
            String recipe = todo.pop();
            if (reachable.add(recipe)) {
                todo.addAll(recipeLists.getOrDefault(recipe, List.of()));
            }
        }

        assertTrue(recipeLists.containsKey(LTS_RECIPE), LTS_RECIPE + " not found");
        assertTrue(!patchRecipes.isEmpty(), "no 4.22.x patch recipe found");
        for (String recipe : patchRecipes) {
            assertTrue(recipeLists.get(LTS_RECIPE).contains(recipe), recipe + " is not chained in 4.22LTS.yaml");
        }
        for (String recipe : INHERITED) {
            assertTrue(reachable.contains(recipe), recipe + " is not reachable from " + LTS_RECIPE);
        }
        List<String> camel423 = reachable.stream().filter(r -> r.startsWith(PREFIX + "camel423")).toList();
        assertTrue(camel423.isEmpty(), "4.23 recipes reachable from " + LTS_RECIPE + ": " + camel423);
    }
}
