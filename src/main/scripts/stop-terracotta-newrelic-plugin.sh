#!/bin/sh

echo "Shutting down Terracotta New Relic plug-in..."
kill -9 `cat tc-nr-plugin.pid`