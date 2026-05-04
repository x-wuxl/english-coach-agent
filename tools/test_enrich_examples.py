import json
import tempfile
import unittest
from pathlib import Path

try:
    from .enrich_examples import (
        ExampleCandidate,
        build_examples_json,
        build_tatoeba_index,
        generate_update_sql,
        load_learning_items,
        match_examples,
        next_flyway_version,
        normalize_match_phrase,
        parse_seed_insert_rows,
        sql_quote,
        validate_generated_sql,
    )
except ImportError:
    from enrich_examples import (
        ExampleCandidate,
        build_examples_json,
        build_tatoeba_index,
        generate_update_sql,
        load_learning_items,
        match_examples,
        next_flyway_version,
        normalize_match_phrase,
        parse_seed_insert_rows,
        sql_quote,
        validate_generated_sql,
    )


class SeedSqlParsingTest(unittest.TestCase):
    def test_parse_seed_values_handles_escaped_quotes_commas_and_newlines(self):
        sql = """INSERT INTO learning_item (item_code, type, content, meaning_zh, difficulty, theme, tags, examples) VALUES
        ('w_000001', 'WORD', '''d better', '最好是；其后加不带；更好的', 1, 'textbook_primary', '["momo"]', '[]'),
        ('w_000002', 'WORD', 'contents of one''s home', 'meaning, with comma
and newline', 5, 'ielts', '[]', '[]');
        """

        rows = list(parse_seed_insert_rows(sql))

        self.assertEqual(rows[0]["item_code"], "w_000001")
        self.assertEqual(rows[0]["content"], "'d better")
        self.assertEqual(rows[1]["content"], "contents of one's home")

    def test_load_learning_items_maps_duplicate_normalized_content_to_all_codes(self):
        with tempfile.TemporaryDirectory() as tmp:
            migration_dir = Path(tmp)
            (migration_dir / "V4__seed_vocabulary_part1.sql").write_text(
                """INSERT INTO learning_item (item_code, type, content, meaning_zh, difficulty, theme, tags, examples) VALUES
                ('w_000001', 'WORD', 'Apple', '', 1, 'theme', '[]', '[]'),
                ('p_000001', 'PHRASE', ' apple ', '', 1, 'theme', '[]', '[]');
                """,
                encoding="utf-8",
            )

            items = load_learning_items(migration_dir)

        self.assertEqual(items["apple"], ["w_000001", "p_000001"])


class TatoebaMatchingTest(unittest.TestCase):
    def test_build_tatoeba_index_keeps_bidirectional_english_chinese_links(self):
        with tempfile.TemporaryDirectory() as tmp:
            tatoeba_dir = Path(tmp)
            (tatoeba_dir / "sentences.csv").write_text(
                "\n".join(
                    [
                        "1\teng\tI like apples.",
                        "2\tcmn\t我喜欢苹果。",
                        "3\teng\tShe will make a speech tomorrow.",
                        "4\tcmn\t她明天要演讲。",
                    ]
                ),
                encoding="utf-8",
            )
            (tatoeba_dir / "links.csv").write_text("1\t2\n4\t3\n", encoding="utf-8")

            index = build_tatoeba_index(tatoeba_dir, min_sentence_len=1, max_sentence_len=120)

        self.assertEqual(index.en_to_zh[1], [2])
        self.assertEqual(index.en_to_zh[3], [4])
        self.assertEqual(index.stats.english_with_zh, 2)

    def test_match_examples_uses_token_boundaries_for_words(self):
        with tempfile.TemporaryDirectory() as tmp:
            tatoeba_dir = Path(tmp)
            (tatoeba_dir / "sentences.csv").write_text(
                "\n".join(
                    [
                        "1\teng\tThe cat sat down.",
                        "2\tcmn\t猫坐下了。",
                        "3\teng\tWe catalog every item.",
                        "4\tcmn\t我们给每件物品编目。",
                    ]
                ),
                encoding="utf-8",
            )
            (tatoeba_dir / "links.csv").write_text("1\t2\n3\t4\n", encoding="utf-8")
            index = build_tatoeba_index(tatoeba_dir, min_sentence_len=1, max_sentence_len=120)

        matches, stats = match_examples({"cat": ["w_000001"]}, index, max_examples=3)

        self.assertEqual([example.en for example in matches["cat"]], ["The cat sat down."])
        self.assertEqual(stats["duplicate_sentence"], 0)

    def test_match_examples_handles_phrases_and_skips_templates(self):
        with tempfile.TemporaryDirectory() as tmp:
            tatoeba_dir = Path(tmp)
            (tatoeba_dir / "sentences.csv").write_text(
                "\n".join(
                    [
                        "1\teng\tShe will make a speech tomorrow.",
                        "2\tcmn\t她明天要演讲。",
                        "3\teng\tA basket of apples is on the table.",
                        "4\tcmn\t桌上有一篮苹果。",
                    ]
                ),
                encoding="utf-8",
            )
            (tatoeba_dir / "links.csv").write_text("1\t2\n3\t4\n", encoding="utf-8")
            index = build_tatoeba_index(tatoeba_dir, min_sentence_len=1, max_sentence_len=120)

        matches, stats = match_examples(
            {"make a speech": ["p_000001"], "a basket of ...": ["p_000002"]},
            index,
            max_examples=3,
        )

        self.assertEqual([example.en for example in matches["make a speech"]], ["She will make a speech tomorrow."])
        self.assertNotIn("a basket of ...", matches)
        self.assertEqual(stats["template_phrase"], 1)

    def test_normalize_match_phrase_trims_placeholder_dots(self):
        self.assertEqual(normalize_match_phrase("take care of sb."), "take care of sb")
        self.assertEqual(normalize_match_phrase("be interested in sth."), "be interested in sth")


class SqlGenerationTest(unittest.TestCase):
    def test_build_examples_json_preserves_chinese_and_drops_full_examples_to_fit(self):
        examples = [
            ExampleCandidate(en="I can't go.", zh="我不能去。", sentence_id=1, match_count=1),
            ExampleCandidate(en="A" * 4090, zh="太长。", sentence_id=2, match_count=1),
        ]

        json_text = build_examples_json(examples, max_chars=120)
        decoded = json.loads(json_text)

        self.assertLessEqual(len(json_text), 120)
        self.assertEqual(decoded, [{"en": "I can't go.", "zh": "我不能去。"}])
        self.assertIn("我不能去", json_text)

    def test_sql_quote_escapes_single_quotes(self):
        self.assertEqual(sql_quote("I can't"), "'I can''t'")

    def test_generate_update_sql_escapes_json_and_item_code(self):
        statement = generate_update_sql(
            "w_00'001",
            [ExampleCandidate(en="I can't go.", zh="我不能去。", sentence_id=1, match_count=1)],
        )

        self.assertIn("I can''t go", statement)
        self.assertIn("WHERE item_code = 'w_00''001'", statement)

    def test_next_flyway_version_and_collision_detection(self):
        with tempfile.TemporaryDirectory() as tmp:
            migration_dir = Path(tmp)
            (migration_dir / "V21__create_coach_memory_tables.sql").write_text("-- ok\n", encoding="utf-8")

            self.assertEqual(next_flyway_version(migration_dir), 22)

            with self.assertRaises(ValueError):
                next_flyway_version(migration_dir, start_version=21, file_count=1)

    def test_validate_generated_sql_checks_json_shape_and_length(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "V22__enrich_examples_with_tatoeba_part1.sql"
            path.write_text(
                """-- Enrich learning_item examples with Tatoeba sentence pairs.
UPDATE learning_item SET examples = '[{"en":"I can''t go.","zh":"我不能去。"}]' WHERE item_code = 'w_000001';
""",
                encoding="utf-8",
            )

            result = validate_generated_sql(path)

        self.assertEqual(result.files, 1)
        self.assertEqual(result.update_statements, 1)
        self.assertEqual(result.json_examples, 1)


if __name__ == "__main__":
    unittest.main()
