import argparse
import json
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "app" / "src" / "main" / "assets" / "data.json"
FACT_IMAGES_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "images" / "facts"
CATEGORY_IMAGES_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "images"

SHORT_DESCRIPTION_RANGE = range(4, 8)
DETAILS_RANGE = range(25, 36)


def word_count(text: str) -> int:
    return len(text.split())


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate the Top Seven content database.")
    parser.add_argument(
        "--category",
        help="Validate only the category with this id (for staged content audits).",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    with DATA_PATH.open("r", encoding="utf-8") as file:
        data = json.load(file)

    if args.category:
        data = [category for category in data if category["id"] == args.category]
        if not data:
            print(f"Unknown category: {args.category}")
            return 2

    errors: list[str] = []
    seen_category_ids: set[str] = set()
    # Identyfikatory faktów muszą być unikalne globalnie, nie tylko w obrębie kategorii.
    # Wcześniej sprawdzana była wyłącznie unikalność lokalna, przez co ta sama siódemka
    # numerów powtarzała się w kilku kategoriach.
    fact_id_owners: dict[int, str] = {}

    for category in data:
        category_id = category["id"]
        if category_id in seen_category_ids:
            errors.append(f"[{category_id}] duplicate category id")
        seen_category_ids.add(category_id)

        if len(category["facts"]) != 7:
            errors.append(f"[{category_id}] fact count: {len(category['facts'])}, expected 7")

        icon = category.get("icon")
        if not icon or not (CATEGORY_IMAGES_DIR / icon).is_file():
            errors.append(f"[{category_id}] missing category image: {icon!r}")

        seen_fact_ids: set[int] = set()
        for fact in category["facts"]:
            fact_id = fact["id"]
            prefix = f"[{category_id}:{fact_id}]"

            if fact_id in seen_fact_ids:
                errors.append(f"{prefix} duplicate fact id inside category")
            seen_fact_ids.add(fact_id)

            owner = fact_id_owners.get(fact_id)
            if owner is not None:
                errors.append(f"{prefix} fact id already used by category {owner!r}")
            fact_id_owners[fact_id] = category_id

            short_len = word_count(fact["shortDescription"])
            details_len = word_count(fact["details"])
            if short_len not in SHORT_DESCRIPTION_RANGE:
                errors.append(f"{prefix} shortDescription words: {short_len}, expected 4-7")
            if details_len not in DETAILS_RANGE:
                errors.append(f"{prefix} details words: {details_len}, expected 25-35")

            image_url = fact.get("imageUrl")
            if not image_url or not (FACT_IMAGES_DIR / image_url).is_file():
                errors.append(f"{prefix} missing fact image: {image_url!r}")

            glossary = fact.get("glossary") or {}
            if not glossary:
                errors.append(f"{prefix} glossary must contain at least one term")

            details_casefold = fact["details"].casefold()
            for term, entry in glossary.items():
                if term.casefold() not in details_casefold:
                    errors.append(f"{prefix} glossary term absent from details: {term!r}")
                if not term.strip():
                    errors.append(f"{prefix} glossary contains an empty term")
                if not entry.get("title", "").strip():
                    errors.append(f"{prefix} glossary term {term!r} has an empty title")
                if not entry.get("definition", "").strip():
                    errors.append(f"{prefix} glossary term {term!r} has an empty definition")

    if errors:
        print(f"Validation failed: {len(errors)} problem(s)")
        for error in errors:
            print(f"- {error}")
        return 1

    fact_count = sum(len(category["facts"]) for category in data)
    print(f"Validation passed: {len(data)} categories, {fact_count} facts.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
