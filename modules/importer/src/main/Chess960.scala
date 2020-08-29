package lila.importer

import chess._

private object Chess960 {

  def isStartPosition(board: Board) =
    board valid {

      def rankMatches0(f: Option[Piece] => Boolean)(rank: Int) =
        (0 to 7) forall { file =>
          f(board.apply0(file, rank))
        }

      rankMatches0 {
        case Some(Piece(White, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(0) &&
      rankMatches0 {
        case Some(Piece(White, Pawn)) => true
        case _                        => false
      }(1) &&
      List(2, 3, 4, 5).forall(rankMatches0(_.isEmpty)) &&
      rankMatches0 {
        case Some(Piece(Black, Pawn)) => true
        case _                        => false
      }(6) &&
      rankMatches0 {
        case Some(Piece(Black, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(7)
    }

  def fixVariantName(v: String) =
    v.toLowerCase match {
      case "chess 960"   => "chess960"
      case "fisherandom" => "chess960" // I swear, sometimes...
      case _             => v
    }
}
