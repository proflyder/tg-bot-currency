#!/bin/bash

# ═══════════════════════════════════════════════════════════════════
# Documentation Validation Script
# ═══════════════════════════════════════════════════════════════════
#
# Проверяет:
#   - Наличие обязательных файлов из TOC.yml
#   - Корректность ссылок в Markdown
#   - Структуру документации
#
# ═══════════════════════════════════════════════════════════════════

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Paths
DOCS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../docs" && pwd)"
TOC_FILE="$DOCS_DIR/TOC.yml"

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# Functions
print_header() {
    echo -e "${CYAN}════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  Documentation Validation${NC}"
    echo -e "${CYAN}════════════════════════════════════════════════${NC}"
    echo ""
}

check() {
    local check_name="$1"
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -n "[$TOTAL_CHECKS] Checking: $check_name... "
}

pass() {
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    echo -e "${GREEN}✓ PASS${NC}"
}

fail() {
    local message="${1:-}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    echo -e "${RED}✗ FAIL${NC}"
    if [ -n "$message" ]; then
        echo -e "    ${RED}↳ $message${NC}"
    fi
}

warn() {
    local message="$1"
    echo -e "${YELLOW}⚠ WARN: $message${NC}"
}

# Check 1: TOC.yml exists
check_toc_exists() {
    check "TOC.yml exists"
    if [ -f "$TOC_FILE" ]; then
        pass
    else
        fail "TOC.yml not found at $TOC_FILE"
    fi
}

# Check 2: index.md exists
check_index_exists() {
    check "index.md exists"
    if [ -f "$DOCS_DIR/index.md" ]; then
        pass
    else
        fail "index.md not found"
    fi
}

# Check 3: Required directories exist
check_required_dirs() {
    local dirs=("guides" "architecture" "deployment" "scripts" "api" "troubleshooting")

    for dir in "${dirs[@]}"; do
        check "Directory docs/$dir/ exists"
        if [ -d "$DOCS_DIR/$dir" ]; then
            pass
        else
            fail "Directory not found"
        fi
    done
}

# Check 4: Required files from TOC.yml
check_required_files() {
    check "Required files from TOC.yml"

    local required_files=(
        "../README.md"
        "index.md"
        "guides/QUICKSTART.md"
        "guides/LOGGING.md"
        "deployment/DOCKER-VOLUMES.md"
        "deployment/CI-CD-GUIDE.md"
        "scripts/README.md"
    )

    local missing_count=0

    for file in "${required_files[@]}"; do
        local full_path="$DOCS_DIR/$file"
        if [ ! -f "$full_path" ]; then
            if [ $missing_count -eq 0 ]; then
                fail "Some required files are missing:"
            fi
            echo -e "    ${RED}↳ Missing: $file${NC}"
            missing_count=$((missing_count + 1))
        fi
    done

    if [ $missing_count -eq 0 ]; then
        pass
    fi
}

# Check 5: Markdown files have titles
check_markdown_titles() {
    check "Markdown files have H1 titles"

    local files_without_titles=0

    while IFS= read -r -d '' file; do
        if ! grep -q "^# " "$file"; then
            if [ $files_without_titles -eq 0 ]; then
                fail "Some files missing H1 title:"
            fi
            echo -e "    ${RED}↳ $(basename "$file")${NC}"
            files_without_titles=$((files_without_titles + 1))
        fi
    done < <(find "$DOCS_DIR" -name "*.md" -print0)

    if [ $files_without_titles -eq 0 ]; then
        pass
    fi
}

# Check 6: No broken internal links (basic check)
check_internal_links() {
    check "Internal Markdown links"

    local broken_links=0

    while IFS= read -r -d '' file; do
        # Remove code blocks before checking links
        local content=$(awk '
            /^```/ { in_code = !in_code; next }
            !in_code { print }
        ' "$file")

        # Extract markdown links: [text](path) from content without code blocks
        while IFS= read -r link; do
            # Remove markdown syntax
            local path=$(echo "$link" | sed -n 's/.*](\([^)]*\)).*/\1/p')

            # Skip external links
            if [[ "$path" =~ ^https?:// ]]; then
                continue
            fi

            # Skip anchors
            if [[ "$path" =~ ^# ]]; then
                continue
            fi

            # Skip example/placeholder paths
            if [[ "$path" =~ ^path/to/ ]] || [[ "$path" =~ \.\*\\ ]]; then
                continue
            fi

            # Resolve relative path
            local dir=$(dirname "$file")
            local full_path="$dir/$path"

            # Remove anchor if present
            full_path="${full_path%%#*}"

            if [ -n "$path" ] && [ ! -f "$full_path" ]; then
                if [ $broken_links -eq 0 ]; then
                    fail "Some internal links are broken:"
                fi
                echo -e "    ${RED}↳ $(basename "$file"): $path${NC}"
                broken_links=$((broken_links + 1))
            fi
        done < <(echo "$content" | grep -o '\[.*\](.*\.md[^)]*)' 2>/dev/null || true)
    done < <(find "$DOCS_DIR" -name "*.md" -print0)

    if [ $broken_links -eq 0 ]; then
        pass
    fi
}

# Check 7: Count total documentation
count_docs() {
    local md_count=$(find "$DOCS_DIR" -name "*.md" | wc -l | tr -d ' ')
    local total_size=$(du -sh "$DOCS_DIR" 2>/dev/null | cut -f1)

    echo ""
    echo -e "${CYAN}────────────────────────────────────────────────${NC}"
    echo -e "Total Markdown files: ${GREEN}$md_count${NC}"
    echo -e "Documentation size: ${GREEN}$total_size${NC}"
}

# Summary
print_summary() {
    echo ""
    echo -e "${CYAN}════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  Validation Summary${NC}"
    echo -e "${CYAN}════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "Total checks: ${CYAN}$TOTAL_CHECKS${NC}"
    echo -e "Passed: ${GREEN}$PASSED_CHECKS${NC}"
    echo -e "Failed: ${RED}$FAILED_CHECKS${NC}"
    echo ""

    if [ $FAILED_CHECKS -eq 0 ]; then
        echo -e "${GREEN}✓ All checks passed!${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}✗ Some checks failed!${NC}"
        echo ""
        return 1
    fi
}

# Main
main() {
    print_header

    check_toc_exists
    check_index_exists
    check_required_dirs
    check_required_files
    check_markdown_titles
    check_internal_links

    count_docs
    print_summary
}

# Run
main
exit $?
