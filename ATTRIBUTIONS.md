# Attributions & Licences tierces

## KanjiAlive
Les données de kanji (lectures, exemples, audio)
proviennent de [KanjiAlive](https://kanjialive.com/), publiées sous licence
[Creative Commons Attribution 4.0 International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/).

Source : https://github.com/kanjialive
         https://github.com/kanjialive/kanji-data-media

> Note : les images statiques (poster SVG) et vidéos de tracé de KanjiAlive
> ont été remplacées par les SVGs animés d'animCJK (voir ci-dessous).
> Les données textuelles (lectures, exemples, audio) sont conservées.
 
 ## animCJK
Les SVGs de tracé animé des kanji et kana (stroke order) sont issus du projet
[animCJK](https://github.com/parsimonhi/animCJK) de Tung-Han Hsieh (parsimonhi).
 
Licences mixtes selon l'origine des glyphes :
- Kanji basés sur KanjiVG : **Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)**
- Kana basés sur la police Arphic : **Arphic Public License**
- Autres glyphes : **GNU Lesser General Public License (LGPL)**
 
Source : https://github.com/parsimonhi/animCJK
 
Les fichiers SVGs sont stockés localement dans `backend/src/main/resources/static/animcjk/`
et ne sont pas versionnés dans ce dépôt (voir `.gitignore`).

## KANJIDIC2, JMdict, et fichiers associés — EDRDG
Les fichiers KANJIDIC2, JMdict, les fichiers de décomposition de kanji (`kradfile`, `kradfile2`, `radkfile`, `radkfile2`) sont la propriété de James William Breen et du Electronic Dictionary Research and Development Group (EDRDG).
Publiés sous licence Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).
Les données dérivées de ces fichiers sont redistribuées sous la même licence CC BY-SA 4.0.
Source : https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project   
         https://www.edrdg.org/kanjidic/kanjd2index_legacy.html

## Niveaux JLPT pour le vocabulaire
Les niveaux JLPT associés aux mots du vocabulaire sont issus du projet
[yomitan-jlpt-vocab](https://github.com/stephenmk/yomitan-jlpt-vocab) de Stephen Kraus,
lui‑même basé sur les listes de Jonathan Waller, publiées sous licence
[Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)](https://creativecommons.org/licenses/by-sa/4.0/).
Source : https://github.com/stephenmk/yomitan-jlpt-vocab