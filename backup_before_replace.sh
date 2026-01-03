#!/bin/bash
# backup_before_replace.sh
cd /home/niyid/git/apo
BACKUP_DIR="$HOME/Documents/apo_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -r app/src/main/java/com/techducat/apo "$BACKUP_DIR/"
echo "Backup created at: $BACKUP_DIR"
echo "To restore: cp -r $BACKUP_DIR/* app/src/main/java/com/techducat/apo/"
