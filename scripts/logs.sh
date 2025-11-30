#!/bin/bash

# ═══════════════════════════════════════════════════════════════════
# Утилита для управления логами Currency Bot
# ═══════════════════════════════════════════════════════════════════

set -e

CONTAINER_NAME="currency-bot"
LOG_FILE="/app/logs/currency-bot.log"
VOLUME_NAME="currency-bot-logs"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Functions
print_help() {
    echo -e "${CYAN}Currency Bot - Logs Management Utility${NC}"
    echo ""
    echo "Usage: ./logs.sh [command]"
    echo ""
    echo "Commands:"
    echo "  tail          Follow logs in real-time"
    echo "  show          Show last 100 lines of logs"
    echo "  save [file]   Save logs to file (default: ./logs-backup.txt)"
    echo "  size          Show logs size"
    echo "  clean [days]  Delete logs older than X days (default: 30)"
    echo "  backup [file] Create backup archive (default: ./logs-backup-DATE.tar.gz)"
    echo "  restore [file] Restore logs from backup"
    echo "  list          List all log files in volume"
    echo "  volume        Show volume info"
    echo "  help          Show this help"
}

check_container() {
    if ! docker ps -q -f name="$CONTAINER_NAME" > /dev/null 2>&1; then
        echo -e "${RED}Error: Container '$CONTAINER_NAME' is not running${NC}"
        exit 1
    fi
}

tail_logs() {
    check_container
    echo -e "${GREEN}Following logs (Ctrl+C to stop)...${NC}"
    docker exec -it "$CONTAINER_NAME" tail -f "$LOG_FILE"
}

show_logs() {
    check_container
    echo -e "${GREEN}Last 100 lines of logs:${NC}"
    docker exec "$CONTAINER_NAME" tail -n 100 "$LOG_FILE"
}

save_logs() {
    local output_file="${1:-./logs-backup.txt}"
    check_container
    echo -e "${GREEN}Saving logs to $output_file...${NC}"
    docker cp "$CONTAINER_NAME:$LOG_FILE" "$output_file"
    echo -e "${GREEN}Done! Saved to: $output_file${NC}"
}

show_size() {
    check_container
    echo -e "${GREEN}Logs size:${NC}"
    docker exec "$CONTAINER_NAME" du -sh /app/logs
    echo ""
    echo -e "${GREEN}Volume size:${NC}"
    docker system df -v | grep "$VOLUME_NAME" || echo "Volume not found"
}

clean_logs() {
    local days="${1:-30}"
    check_container
    echo -e "${YELLOW}Deleting logs older than $days days...${NC}"
    docker exec "$CONTAINER_NAME" find /app/logs -name "*.log" -mtime +"$days" -delete
    echo -e "${GREEN}Done!${NC}"
}

backup_logs() {
    local output_file="${1:-./logs-backup-$(date +%Y%m%d-%H%M%S).tar.gz}"
    echo -e "${GREEN}Creating backup archive...${NC}"
    docker run --rm \
        -v "$VOLUME_NAME":/source \
        -v "$(pwd)":/backup \
        alpine tar czf "/backup/$(basename "$output_file")" -C /source .
    echo -e "${GREEN}Done! Backup saved to: $output_file${NC}"
}

restore_logs() {
    local input_file="$1"
    if [ -z "$input_file" ]; then
        echo -e "${RED}Error: Please specify backup file${NC}"
        echo "Usage: ./logs.sh restore <backup-file.tar.gz>"
        exit 1
    fi

    if [ ! -f "$input_file" ]; then
        echo -e "${RED}Error: File $input_file not found${NC}"
        exit 1
    fi

    echo -e "${YELLOW}Warning: This will replace current logs!${NC}"
    read -p "Continue? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        exit 1
    fi

    echo -e "${GREEN}Restoring logs from backup...${NC}"
    docker run --rm \
        -v "$VOLUME_NAME":/target \
        -v "$(pwd)":/backup \
        alpine tar xzf "/backup/$(basename "$input_file")" -C /target
    echo -e "${GREEN}Done!${NC}"
}

list_logs() {
    check_container
    echo -e "${GREEN}Log files in volume:${NC}"
    docker exec "$CONTAINER_NAME" ls -lah /app/logs/
}

show_volume() {
    echo -e "${GREEN}Volume info:${NC}"
    docker volume inspect "$VOLUME_NAME"
}

# Main
case "${1:-help}" in
    tail)
        tail_logs
        ;;
    show)
        show_logs
        ;;
    save)
        save_logs "$2"
        ;;
    size)
        show_size
        ;;
    clean)
        clean_logs "$2"
        ;;
    backup)
        backup_logs "$2"
        ;;
    restore)
        restore_logs "$2"
        ;;
    list)
        list_logs
        ;;
    volume)
        show_volume
        ;;
    help|*)
        print_help
        ;;
esac
