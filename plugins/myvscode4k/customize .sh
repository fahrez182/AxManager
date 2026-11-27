#!/system/bin/sh
SKIPUNZIP=1
ui_print "- Detecting display refresh rate capabilities..."

# Create a temporary directory to store the detected rates
RATE_FILE=/data/local/tmp/supported_rates.txt
> "$RATE_FILE"

# Use dumpsys to find all supported refresh rates
dumpsys display | grep -oE 'fps=[0-9]+(\.[0-9]+)?' | cut -d'=' -f2 | sort -u > "$RATE_FILE"

if [ -s "$RATE_FILE" ]; then
    ui_print "- Supported rates detected:"
    cat "$RATE_FILE" | while read rate; do
        ui_print "  - ${rate} Hz"
    done
else
    ui_print "! Could not detect refresh rates. Module may not function correctly."
fi

ui_print "- Installation complete. Reboot to apply."
