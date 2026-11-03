# Ralph Loop Agent Instructions

You are executing the BrainPal APK reconstruction iteratively against `PRD.md`.
Follow these guidelines strictly:
1. Check `progress.txt` to find the current unfinished task.
2. Complete each task thoroughly before advancing.
3. For each iteration:
   - Mark task started in `progress.txt` with timestamp.
   - Inspect files and execute required commands.
   - Run verification and tests.
   - Fix any failures at root cause.
   - Mark task completed in `progress.txt` with timestamp.
4. Ensure all changes are modular, clean, and well-tested.
5. Do not skip tasks or stop prematurely.

