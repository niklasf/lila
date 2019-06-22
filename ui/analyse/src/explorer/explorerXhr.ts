import { OpeningData, TablebaseData, ExplorerData } from './interfaces';

export function opening(endpoint: string, variant: VariantKey, fen: Fen, config, withGames: boolean): JQueryPromise<OpeningData> {
  let url: string;
  const params: any = {
    fen,
    moves: 12
  };
  if (!withGames) params.topGames = params.recentGames = 0;
  if (config.db.selected() === 'masters') url = '/master';
  else {
    url = '/lichess';
    params['variant'] = variant;
    params['speeds[]'] = config.speed.selected();
    params['ratings[]'] = config.rating.selected();
  }
  return $.ajax({
    url: endpoint + url,
    data: params,
    cache: true
  }).then((data: Partial<OpeningData>) => {
    data.opening = true;
    data.fen = fen;
    return data as OpeningData;
  });
}

export function tablebase(endpoint: string, variant: VariantKey, fen: Fen): JQueryPromise<TablebaseData> {
  const effectiveVariant = (variant === 'fromPosition' || variant === 'chess960') ? 'standard' : variant;
  return $.ajax({
    url: endpoint + '/' + effectiveVariant,
    data: { fen },
    cache: true
  }).then((data: Partial<TablebaseData>) => {
    data.tablebase = true;
    data.fen = fen;
    return data as TablebaseData;
  });
}

export function chessdb(fen: Fen): JQueryPromise<ExplorerData> {
  return $.ajax({
    url: 'https://www.chessdb.cn/cdb.php',
    data: {
      action: 'queryall',
      board: fen,
      json: 1
    },
    cache: true
  }).then((data: Partial<ExplorerData>) => {
    data.chessdb = true;
    data.fen = fen;
    console.log(data);
    return data as ExplorerData;
  });
}
