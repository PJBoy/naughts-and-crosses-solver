/*
---  0---|--- 10---|--- 20---|--- 30---|--- 40---|--- 50---|--- 60---|--- 70---
123456789|123456789|123456789|123456789|123456789|123456789|123456789|123456789
*/
class Board
{
    // 18-bit board: X0X0X0...
    //  Access with 1<<(y*3+x)*2<<player
    private int board;
    private int cellsUsed;
    // 0=0, 1=X
    private int currentPlayer;

    // Everything's initialised to zero (currentPlayer is zeroed on first move)
    Board(){}

    Position position(String input)
    {
        int x, y;
        // Check for: Two characters, in range a-c and 0-2 (case-sensitive),
        //  and not taken
        if
            (
            input.length()!=2 ||
            (y=input.charAt(0)-'a')>2 || y<0 ||
            (x=input.charAt(1)-'1')>2 || x<0 ||
            (board & 0x3 << (y*3 + x)*2) != 0
            )
            return null;
        return new Position(y, x);
    }

    void move(Position position)
    {
        // The motive for pre-toggling currentPlayer is so it's correct for the
        //  other methods
        currentPlayer = 1 - currentPlayer;
        board |= 1 << ((position.row()*3 + position.col())*2 + currentPlayer);
        ++cellsUsed;
    }

    int winner_i(int player)
    {
        // Returns -1 for 0 winning and 1 for X, 0 for a draw, and 2 otherwise
        if (cellsUsed < 5)
            return 2;
        // Here's the motive for using a bitfield to represent the board:
        //  The eight possible winning combinations are represented by masks
        //  (see EOF).
        if
            (
            (board>>player&0x01110)==0x01110 ||
            (board>>player&0x10101)==0x10101 ||
            (board>>player&0x00015)==0x00015 ||
            (board>>player&0x00540)==0x00540 ||
            (board>>player&0x01041)==0x01041 ||
            (board>>player&0x04104)==0x04104 ||
            (board>>player&0x10410)==0x10410 ||
            (board>>player&0x15000)==0x15000
            )
            return player*2 - 1;
        else if (cellsUsed == 9)
            return 0;
        return 2;
    }

    Player winner()
    {
        return new Player[]
        {
            Player.O, Player.Both, Player.X, Player.None
        }
        [winner_i(currentPlayer)+1];
    }

    Position[] blanks()
    {
        Position positions[] = new Position[9-cellsUsed];
        // Unused entries becomes set, used entries cleared
        int boardCopy = (board>>1 | board) & 0x15555 ^ 0x15555;
        for (int i = 0, j = 0; boardCopy != 0; ++i, boardCopy >>= 2)
        {
            if ((boardCopy & 0x1) != 0)
                positions[j++] = new Position(i/3, i%3);
        }
        return positions;
    }

    public String toString()
    {
        char boardSymbols[] = {' ', '0', 'X'};
        String boardString = new String("     1   2   3\n\n");
        for (int i = 0; i < 3; ++i)
        {
            boardString += " " + (char)('a'+i) + "   " +
                boardSymbols[board>>i*6 & 0x3] + " | " +
                boardSymbols[board>>i*6+2 & 0x3] + " |";
            if ((board>>i*6+4 & 0x3) != 0)
                boardString += " " + boardSymbols[board>>i*6+4 & 0x3];
            boardString += "\n";
            if (i != 2)
                boardString += "    ---+---+---\n";
        }
        return boardString;
    }

    Position suggest_r(int player, int[] result)
    {
        // Minimum works as so: Opponent wins = -1, draw = 0, win = 1
        int minimum = -2, best = -1;
        for (int move = 0; move < 9 && minimum != 1; ++move)
        {
            // Check that space is available
            if ((board >> move*2 & 0x3) != 0)
                continue;
            // Simulate move
            board |= 1 << move*2+player;
            ++cellsUsed;
            result[0] = winner_i(player);
            // Does move end game?
            if (result[0] != 2)
            {
                // (Undo move)
                board &= ~(0x3 << move*2);
                --cellsUsed;
                // Then return as solution
                return new Position(move/3, move%3);
            }
            // Else simulate oppoent's suggested move
            suggest_r(1 - player, result);
            // If a greater minimum:
            if (minimum < result[0] * (2*player-1))
            {
                minimum = result[0] * (2*player-1);
                // Then use as current best move
                best = move;
            }
            // (Undo move)
            board &= ~(0x3 << move*2);
            --cellsUsed;
        }
        // Return best result for this player
        result[0] = minimum * (2*player-1);
        // Return best position found so far
        return new Position(best/3, best%3);
    }

    Position suggest()
    {
        // Call a recursive function.
        // When this function is called, it will still be the user's turn
        // Using a singleton to pass-by-reference
        int[] result = new int[1];
        return suggest_r(1 - currentPlayer, result);
    }
}

/*
111 000 100 001
000 000 100 001
000 111 100 001

000 010 100 001
111 010 010 010
000 010 001 100
---------------------------
 3  2 3  2 4  2 3  2 3
     01 0001 0001 0000 = 01110
01 0000 0001 0000 0001 = 10101
               01 0101 = 00015
        0101 0100 0000 = 00540
     01 0000 0100 0001 = 01041
   0100 0001 0000 0100 = 04104
01 0000 0100 0001 0000 = 10410
01 0101 0000 0000 0000 = 15000
*/
