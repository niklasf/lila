import requests
import os
import shutil

print("diff | prod | new | file size of")
print("--- | --- | --- | --- | ---")
total_prod = total = 0

def compare(url, path, tmp_name):
    global total_prod, total

    try:
        prod_size = os.stat(tmp_name).st_size
    except OSError:
        req = requests.get(url, stream=True)
        assert req.status_code == 200
        with open(tmp_name, "wb") as f:
            req.raw.decode_content = True
            shutil.copyfileobj(req.raw, f)

        prod_size = os.stat(tmp_name).st_size

    size = os.stat(path).st_size
    print(size - prod_size, "|", prod_size, "|", size, "|", path, "(!!!)" if prod_size < size else "")

    total_prod += prod_size
    total += size

for pset in "alpha cburnett chess7 chessnut companion fantasy letter merida mono pirouetti reillycraig spatial shapes".split():
    if pset == "mono":
        names = "B.svg  K.svg  N.svg  P.svg  Q.svg  R.svg"
    else:
        names = "bB.svg  bK.svg  bN.svg  bP.svg  bQ.svg  bR.svg  wB.svg  wK.svg  wN.svg  wP.svg  wQ.svg  wR.svg"

    for name in names.split():
        url = "https://lichess1.org/assets/piece/%s/%s" % (pset, name)
        path = "public/piece/%s/%s" % (pset, name)
        tmp_name = "/tmp/%s-%s" % (pset, name)
        compare(url, path, tmp_name)

print(total - total_prod, "|", total_prod, "|", total, "|", "**total**")

print()
print("diff | prod | new | file size of")
print("--- | --- | --- | --- | ---")

total_prod = total = 0

for pset in "alpha.css  cburnett.css  chess7.css  chessnut.css  companion.css  fantasy.css  letter.css  merida.css  pirouetti.css  reillycraig.css  shapes.css  spatial.css".split():
    url = "https://lichess1.org/assets/stylesheets/piece/%s" % pset
    path = "public/stylesheets/piece/%s" % pset
    tmp_name = "/tmp/%s" % pset
    compare(url, path, tmp_name)

print(total - total_prod, "|", total_prod, "|", total, "|", "**total**")
