package org.lichess.compression.game;

import java.util.ArrayList;

final class Board {
    final long byRole[] = {
        0xff00000000ff00L, // pawn
        0x4200000000000042L, // knight
        0x2400000000000024L, // bishop
        0x8100000000000081L, // rook
        0x800000000000008L, // queen
        0x1000000000000010L // king
    };

    final long byColor[] = {
        0xffff000000000000L, // black
        0xffffL, // white
    };

    private long occupied = 0xffff00000000ffffL;

    int turn = Color.WHITE;
    int epSquare = 0;
    long castlingRights = 0x8100000000000081L;

    public Board() { }

    public Board(Board board) {
        System.arraycopy(board.byRole, 0, this.byRole, 0, 6);
        System.arraycopy(board.byColor, 0, this.byColor, 0, 2);
        this.occupied = board.occupied;

        this.turn = board.turn;
        this.epSquare = board.epSquare;
        this.castlingRights = board.castlingRights;
    }

    Board(long pawns, long knights, long bishops, long rooks, long queens, long kings,
          long white, long black,
          int turn, int epSquare, long castlingRights) {

        this.byRole[Role.PAWN.index] = pawns;
        this.byRole[Role.KNIGHT.index] = knights;
        this.byRole[Role.BISHOP.index] = bishops;
        this.byRole[Role.ROOK.index] = rooks;
        this.byRole[Role.QUEEN.index] = queens;
        this.byRole[Role.KING.index] = kings;

        this.byColor[Color.BLACK] = black;
        this.byColor[Color.WHITE] = white;
        this.occupied = white | black;

        this.turn = turn;
        this.epSquare = epSquare;
        this.castlingRights = castlingRights;
    }

    private boolean isOccupied(int square) {
        return Bitboard.contains(this.occupied, square);
    }

    private void discard(int square, int color, Role role) {
        long mask = ~(1L << square);
        this.byRole[role.index] &= mask;
        this.byColor[color] &= mask;
        this.occupied &= mask;
    }

    private void put(int square, Role role) {
        // Potentially discard captures piece.
        long capture = ~(1L << square);
        this.byRole[Role.PAWN.index] &= capture;
        this.byRole[Role.KNIGHT.index] &= capture;
        this.byRole[Role.BISHOP.index] &= capture;
        this.byRole[Role.ROOK.index] &= capture;
        this.byRole[Role.QUEEN.index] &= capture;
        this.byRole[Role.KING.index] &= capture;
        this.byColor[Color.BLACK] &= capture;
        this.byColor[Color.WHITE] &= capture;

        // Put new piece.
        long mask = 1L << square;
        this.byRole[role.index] ^= mask;
        this.byColor[this.turn] ^= mask;
        this.occupied |= mask;
    }

    public void play(Move move) {
        this.epSquare = 0;

        switch (move.type) {
            case Move.NORMAL:
                if (move.role == Role.PAWN && Math.abs(move.from - move.to) == 16) {
                    this.epSquare = move.from + (this.turn == Color.WHITE ? 8 : -8);
                }

                if (this.castlingRights != 0) {
                    if (move.role == Role.KING) {
                        this.castlingRights &= Bitboard.RANKS[this.turn == Color.WHITE ? 7 : 0];
                    } else if (move.role == Role.ROOK) {
                        this.castlingRights &= ~(1L << move.from);
                    }

                    if (move.capture) {
                        this.castlingRights &= ~(1L << move.to);
                    }
                }

                discard(move.from, this.turn, move.role);
                put(move.to, move.promotion != null ? move.promotion : move.role);
                break;

            case Move.CASTLING:
                this.castlingRights &= Bitboard.RANKS[this.turn == Color.WHITE ? 7 : 0];
                int rookTo = Square.combine(move.to < move.from ? Square.D1 : Square.F1, move.to);
                int kingTo = Square.combine(move.to < move.from ? Square.C1 : Square.G1, move.from);
                discard(move.from, this.turn, Role.KING);
                discard(move.to, this.turn, Role.ROOK);
                put(rookTo, Role.ROOK);
                put(kingTo, Role.KING);
                break;

            case Move.EN_PASSANT:
                discard(Square.combine(move.to, move.from), this.turn ^ 1, Role.PAWN);
                discard(move.from, this.turn, Role.PAWN);
                put(move.to, Role.PAWN);
                break;
        }

        this.turn ^= 1;
    }

    private long us() {
        return byColor[this.turn];
    }

    private long them() {
        return byColor[this.turn ^ 1];
    }

    private int king(int color) {
        return Bitboard.lsb(this.byRole[Role.KING.index] & byColor[color]);
    }

    private long sliderBlockers(int king) {
        long snipers = them() & (
            Bitboard.rookAttacks(king, 0) & (this.byRole[Role.ROOK.index] ^ this.byRole[Role.QUEEN.index]) |
            Bitboard.bishopAttacks(king, 0) & (this.byRole[Role.BISHOP.index] ^ this.byRole[Role.QUEEN.index]));

        long blockers = 0;

        while (snipers != 0) {
            int sniper = Bitboard.lsb(snipers);
            long between = Bitboard.BETWEEN[king][sniper] & this.occupied;
            if (!Bitboard.moreThanOne(between)) blockers |= between;
            snipers ^= 1L << sniper;
        }

        return blockers;
    }

    public boolean isCheck() {
        return attacksTo(king(this.turn), this.turn ^ 1) != 0;
    }

    private long attacksTo(int sq, int attacker) {
        return attacksTo(sq, attacker, this.occupied);
    }

    private long attacksTo(int sq, int attacker, long occupied) {
        return byColor[attacker] & (
            Bitboard.rookAttacks(sq, occupied) & (this.byRole[Role.ROOK.index] ^ this.byRole[Role.QUEEN.index]) |
            Bitboard.bishopAttacks(sq, occupied) & (this.byRole[Role.BISHOP.index] ^ this.byRole[Role.QUEEN.index]) |
            Bitboard.KNIGHT_ATTACKS[sq] & this.byRole[Role.KNIGHT.index] |
            Bitboard.KING_ATTACKS[sq] & this.byRole[Role.KING.index] |
            Bitboard.pawnAttacks(attacker ^ 1, sq) & this.byRole[Role.PAWN.index]);
    }

    public void legalMoves(ArrayList<Move> moves) {
        moves.clear();

        int king = king(this.turn);
        boolean hasEp = genEnPassant(moves);

        long checkers = attacksTo(king, this.turn ^ 1);
        if (checkers == 0) {
            long target = ~us();
            genNonKing(target, moves);
            genSafeKing(king, target, moves);
            genCastling(king, moves);
        } else {
            genEvasions(king, checkers, moves);
        }

        long blockers = sliderBlockers(king);
        if (blockers != 0 || hasEp) {
            moves.removeIf(m -> !isSafe(king, m, blockers));
        }
    }

    private void genNonKing(long mask, ArrayList<Move> moves) {
        genPawn(mask, moves);

        // Knights.
        long knights = us() & this.byRole[Role.KNIGHT.index];
        while (knights != 0) {
            int from = Bitboard.lsb(knights);
            long targets = Bitboard.KNIGHT_ATTACKS[from] & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.add(Move.normal(this, Role.KNIGHT, from, isOccupied(to), to));
                targets ^= 1L << to;
            }
            knights ^= 1L << from;
        }

        // Bishops.
        long bishops = us() & this.byRole[Role.BISHOP.index];
        while (bishops != 0) {
            int from = Bitboard.lsb(bishops);
            long targets = Bitboard.bishopAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.add(Move.normal(this, Role.BISHOP, from, isOccupied(to), to));
                targets ^= 1L << to;
            }
            bishops ^= 1L << from;
        }

        // Rooks.
        long rooks = us() & this.byRole[Role.ROOK.index];
        while (rooks != 0) {
            int from = Bitboard.lsb(rooks);
            long targets = Bitboard.rookAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.add(Move.normal(this, Role.ROOK, from, isOccupied(to), to));
                targets ^= 1L << to;
            }
            rooks ^= 1L << from;
        }

        // Queens.
        long queens = us() & this.byRole[Role.QUEEN.index];
        while (queens != 0) {
            int from = Bitboard.lsb(queens);
            long targets = Bitboard.queenAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.add(Move.normal(this, Role.QUEEN, from, isOccupied(to), to));
                targets ^= 1L << to;
            }
            queens ^= 1L << from;
        }
    }

    private void genSafeKing(int king, long mask, ArrayList<Move> moves) {
        long targets = Bitboard.KING_ATTACKS[king] & mask;
        while (targets != 0) {
            int to = Bitboard.lsb(targets);
            if (attacksTo(to, this.turn ^ 1) == 0) {
                moves.add(Move.normal(this, Role.KING, king, isOccupied(to), to));
            }
            targets ^= 1L << to;
        }
    }

    private void genEvasions(int king, long checkers, ArrayList<Move> moves) {
        // Checks by these sliding pieces can maybe be blocked.
        long sliders = checkers & (this.byRole[Role.BISHOP.index] ^ this.byRole[Role.ROOK.index] ^ this.byRole[Role.QUEEN.index]);

        // Collect attacked squares that the king can not escape to.
        long attacked = 0;
        while (sliders != 0) {
            int slider = Bitboard.lsb(sliders);
            attacked |= Bitboard.RAYS[king][slider] ^ (1L << slider);
            sliders ^= 1L << slider;
        }

        genSafeKing(king, ~us() & ~attacked, moves);

        if (checkers != 0 && !Bitboard.moreThanOne(checkers)) {
            int checker = Bitboard.lsb(checkers);
            long target = Bitboard.BETWEEN[king][checker] | checkers;
            genNonKing(target, moves);
        }
    }

    private void genPawn(long mask, ArrayList<Move> moves) {
        // Pawn captures (except en passant).
        long capturers = us() & this.byRole[Role.PAWN.index];
        while (capturers != 0) {
            int from = Bitboard.lsb(capturers);
            long targets = Bitboard.pawnAttacks(this.turn, from) & them() & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                addPawnMoves(from, true, to, moves);
                targets ^= 1L << to;
            }
            capturers ^= 1L << from;
        }

        // Normal pawn moves.
        long singleMoves =
            ~this.occupied & (this.turn == Color.WHITE ?
                ((this.byColor[Color.WHITE] & this.byRole[Role.PAWN.index]) << 8) :
                ((this.byColor[Color.BLACK] & this.byRole[Role.PAWN.index]) >>> 8));

        long doubleMoves =
            ~this.occupied &
            (this.turn == Color.WHITE ? (singleMoves << 8) : (singleMoves >>> 8)) &
            Bitboard.RANKS[this.turn == Color.WHITE ? 3 : 4];

        singleMoves &= mask;
        doubleMoves &= mask;

        while (singleMoves != 0) {
            int to = Bitboard.lsb(singleMoves);
            int from = to + (this.turn == Color.WHITE ? -8 : 8);
            addPawnMoves(from, false, to, moves);
            singleMoves ^= 1L << to;
        }

        while (doubleMoves != 0) {
            int to = Bitboard.lsb(doubleMoves);
            int from = to + (this.turn == Color.WHITE ? -16: 16);
            moves.add(Move.normal(this, Role.PAWN, from, false, to));
            doubleMoves ^= 1L << to;
        }
    }

    private void addPawnMoves(int from, boolean capture, int to, ArrayList<Move> moves) {
        if (Square.rank(to) == (this.turn == Color.WHITE ? 7 : 0)) {
            moves.add(Move.promotion(this, from, capture, to, Role.QUEEN));
            moves.add(Move.promotion(this, from, capture, to, Role.KNIGHT));
            moves.add(Move.promotion(this, from, capture, to, Role.ROOK));
            moves.add(Move.promotion(this, from, capture, to, Role.BISHOP));
        } else {
            moves.add(Move.normal(this, Role.PAWN, from, capture, to));
        }
    }

    private boolean genEnPassant(ArrayList<Move> moves) {
        if (this.epSquare == 0) return false;

        boolean found = false;
        long pawns = us() & this.byRole[Role.PAWN.index] & Bitboard.pawnAttacks(this.turn ^ 1, this.epSquare);
        while (pawns != 0) {
            int pawn = Bitboard.lsb(pawns);
            moves.add(Move.enPassant(this, pawn, this.epSquare));
            found = true;
            pawns ^= 1L << pawn;
        }
        return found;
    }

    private void genCastling(int king, ArrayList<Move> moves) {
        long rooks = this.castlingRights & Bitboard.RANKS[this.turn == Color.WHITE ? 0 : 7];
        while (rooks != 0) {
            int rook = Bitboard.lsb(rooks);
            long path = Bitboard.BETWEEN[king][rook];
            if ((path & this.occupied) == 0) {
                int kingTo = Square.combine(rook < king ? Square.C1 : Square.G1, king);
                long kingPath = Bitboard.BETWEEN[king][kingTo] | (1L << kingTo) | (1L << king);
                while (kingPath != 0) {
                    int sq = Bitboard.lsb(kingPath);
                    if (attacksTo(sq, this.turn ^ 1, this.occupied ^ (1L << king)) != 0) {
                        break;
                    }
                    kingPath ^= 1L << sq;
                }
                if (kingPath == 0) moves.add(Move.castle(this, king, rook));
            }
            rooks ^= 1L << rook;
        }
    }

    // Used for filtering candidate moves that would leave/put the king
    // in check.
    private boolean isSafe(int king, Move move, long blockers) {
        switch (move.type) {
            case Move.NORMAL:
                return
                    !Bitboard.contains(us() & blockers, move.from) ||
                    Square.aligned(move.from, move.to, king);

            case Move.EN_PASSANT:
                long occupied = this.occupied;
                occupied ^= (1L << move.from);
                occupied ^= (1L << Square.combine(move.to, move.from));
                occupied |= (1L << move.to);
                return
                    (Bitboard.rookAttacks(king, occupied) & them() & (this.byRole[Role.ROOK.index] ^ this.byRole[Role.QUEEN.index])) == 0 &&
                    (Bitboard.bishopAttacks(king, occupied) & them() & (this.byRole[Role.BISHOP.index] ^ this.byRole[Role.QUEEN.index])) == 0;

            default:
                return true;
        }
    }
}
