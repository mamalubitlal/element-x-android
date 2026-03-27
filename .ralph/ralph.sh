#!/bin/bash
set -e

ITERATIONS=0
MAX_ITERATIONS=50

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator-ralph

echo "🚀 Starting Ralph Loop (чатор Android Integration)..."
echo "Max iterations: $MAX_ITERATIONS"
echo ""

# Initialize state file
STATE_DIR=".opencode/state"
mkdir -p "$STATE_DIR"
echo '{"depth": 0, "iteration": 0, "started_at": "'$(date -Iseconds)'"}' > "$STATE_DIR/iteration-state.json"

while [ $ITERATIONS -lt $MAX_ITERATIONS ]; do
 ITERATIONS=$((ITERATIONS + 1))
 echo ""
 echo "=========================================="
 echo -e "${GREEN}=== Iteration $ITERATIONS ===${NC}"
 echo "=========================================="
 
 # Update state
 echo '{"depth": 0, "iteration": '$ITERATIONS', "started_at": "'$(date -Iseconds)'"}' > "$STATE_DIR/iteration-state.json"
 
 # Run OpenCode with fresh context
 echo -e "${YELLOW}Running OpenCode with PROMPT.md...${NC}"
 if ! cat PROMPT.md | opencode --no-stream 2>&1; then
 echo -e "${RED}❌ OpenCode failed or returned error${NC}"
 # Don't exit — let's see what happened and continue
 fi
 
 # Run backpressure checks
 echo ""
 echo -e "${YELLOW}Running backpressure checks...${NC}"
 
 BACKPRESSURE_OK=true
 
 cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator/chator
 
 # Test suite (if tests exist)
 if [ -d "tests" ] || [ -d "app/src/test" ]; then
 if ./gradlew test --quiet 2>/dev/null; then
 echo -e "${GREEN}✅ Tests passed${NC}"
 else
 echo -e "${RED}❌ Tests failed — next iteration must fix${NC}"
 BACKPRESSURE_OK=false
 fi
 fi
 
 # Linting
 if ./gradlew lint --quiet 2>/dev/null; then
 echo -e "${GREEN}✅ Lint passed${NC}"
 else
 echo -e "${RED}❌ Lint errors found — next iteration must fix${NC}"
 BACKPRESSURE_OK=false
 fi
 
 cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator-ralph
 
 # Check if task is complete
 if grep -q "ALL_ITEMS_COMPLETE\|✅ ALL DONE" fix_plan.md 2>/dev/null; then
 echo ""
 echo -e "${GREEN}✅ All items complete! Ralph Loop finished successfully!${NC}"
 break
 fi
 
 # Commit progress
 echo ""
 echo -e "${YELLOW}Committing progress...${NC}"
 cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator
 git add -A
 if git commit -m "ralph: iteration $ITERATIONS" --allow-empty 2>/dev/null; then
 echo -e "${GREEN}✅ Committed iteration $ITERATIONS${NC}"
 else
 echo -e "${YELLOW}⚠️ No changes to commit${NC}"
 fi
 
 # Save iteration summary
 echo "Iteration $ITERATIONS completed at $(date -Iseconds)" >> progress.txt
 
 cd /mnt/data/openclaw/workspace/.openclaw/workspace/chator-ralph
 echo ""
 echo "📝 Progress saved. Continuing to next iteration..."
done

# Final summary
echo ""
echo "=========================================="
echo -e "${GREEN}🏁 Ralph Loop Finished${NC}"
echo "=========================================="
echo "Total iterations: $ITERATIONS"
echo "Completed at: $(date -Iseconds)"

# Save final state
echo '{"depth": 0, "iteration": '$ITERATIONS', "completed_at": "'$(date -Iseconds)'"}' > "$STATE_DIR/iteration-state.json"
