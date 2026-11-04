# Two-minute demo script

1. Show booking 102 for Jane Smith as CONFIRMED.
2. Ask the chat to cancel booking 102. Note the returned ticket ID and pending state.
3. Enter the operator key, refresh tickets, and approve. Show the booking as CANCELLED.
4. Repeat approval via the API; show that only one EXECUTED audit entry exists.
5. Restart with a fresh demo database or use a different eligible booking, request cancellation, reject, and show that the booking remains CONFIRMED.
6. Show `mvn test` results and explain the original code versus the added workflow.
