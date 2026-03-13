package com.kotobalearn.backend.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JMdictImportService {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String[]> TAG_MAP = new LinkedHashMap<>();
    static {
        // POS
        TAG_MAP.put("noun (common) (futsuumeishi)",                                  new String[]{"n",        "pos"});
        TAG_MAP.put("adjectival nouns or quasi-adjectives (keiyodoshi)",             new String[]{"adj-na",   "pos"});
        TAG_MAP.put("adjective (keiyoushi)",                                         new String[]{"adj-i",    "pos"});
        TAG_MAP.put("adjective (keiyoushi) - yoi/ii class",                         new String[]{"adj-ix",   "pos"});
        TAG_MAP.put("adverb (fukushi)",                                              new String[]{"adv",      "pos"});
        TAG_MAP.put("adverb taking the 'to' particle",                              new String[]{"adv-to",   "pos"});
        TAG_MAP.put("noun or verb acting prenominally",                              new String[]{"adj-f",    "pos"});
        TAG_MAP.put("nouns which may take the genitive case particle 'no'",         new String[]{"adj-no",   "pos"});
        TAG_MAP.put("pre-noun adjectival (rentaishi)",                              new String[]{"adj-pn",   "pos"});
        TAG_MAP.put("'taru' adjective",                                              new String[]{"adj-t",    "pos"});
        TAG_MAP.put("auxiliary",                                                     new String[]{"aux",      "pos"});
        TAG_MAP.put("auxiliary adjective",                                           new String[]{"aux-adj",  "pos"});
        TAG_MAP.put("auxiliary verb",                                                new String[]{"aux-v",    "pos"});
        TAG_MAP.put("conjunction",                                                   new String[]{"conj",     "pos"});
        TAG_MAP.put("copula",                                                        new String[]{"cop",      "pos"});
        TAG_MAP.put("counter",                                                       new String[]{"ctr",      "pos"});
        TAG_MAP.put("expressions (phrases, clauses, etc.)",                         new String[]{"exp",      "pos"});
        TAG_MAP.put("interjection (kandoushi)",                                      new String[]{"int",      "pos"});
        TAG_MAP.put("adverbial noun (fukushitekimeishi)",                            new String[]{"n-adv",    "pos"});
        TAG_MAP.put("proper noun",                                                   new String[]{"n-pr",     "pos"});
        TAG_MAP.put("noun, used as a prefix",                                        new String[]{"n-pref",   "pos"});
        TAG_MAP.put("noun, used as a suffix",                                        new String[]{"n-suf",    "pos"});
        TAG_MAP.put("noun (temporal) (jisoumeishi)",                                new String[]{"n-t",      "pos"});
        TAG_MAP.put("numeric",                                                       new String[]{"num",      "pos"});
        TAG_MAP.put("pronoun",                                                       new String[]{"pn",       "pos"});
        TAG_MAP.put("prefix",                                                        new String[]{"pref",     "pos"});
        TAG_MAP.put("particle",                                                      new String[]{"prt",      "pos"});
        TAG_MAP.put("suffix",                                                        new String[]{"suf",      "pos"});
        TAG_MAP.put("unclassified",                                                  new String[]{"unc",      "pos"});
        TAG_MAP.put("Ichidan verb",                                                  new String[]{"v1",       "pos"});
        TAG_MAP.put("Ichidan verb - kureru special class",                          new String[]{"v1-s",     "pos"});
        TAG_MAP.put("Kuru verb - special class",                                    new String[]{"vk",       "pos"});
        TAG_MAP.put("irregular nu verb",                                             new String[]{"vn",       "pos"});
        TAG_MAP.put("irregular ru verb, plain form ends with -ri",                  new String[]{"vr",       "pos"});
        TAG_MAP.put("intransitive verb",                                             new String[]{"vi",       "pos"});
        TAG_MAP.put("transitive verb",                                               new String[]{"vt",       "pos"});
        TAG_MAP.put("noun or participle which takes the aux. verb suru",            new String[]{"vs",       "pos"});
        TAG_MAP.put("suru verb - included",                                          new String[]{"vs-i",     "pos"});
        TAG_MAP.put("suru verb - special class",                                    new String[]{"vs-s",     "pos"});
        TAG_MAP.put("su verb - precursor to the modern suru",                      new String[]{"vs-c",     "pos"});
        TAG_MAP.put("Ichidan verb - zuru verb (alternative form of -jiru verbs)",  new String[]{"vz",       "pos"});
        TAG_MAP.put("Godan verb - -aru special class",                              new String[]{"v5aru",    "pos"});
        TAG_MAP.put("Godan verb with 'bu' ending",                                  new String[]{"v5b",      "pos"});
        TAG_MAP.put("Godan verb with 'gu' ending",                                  new String[]{"v5g",      "pos"});
        TAG_MAP.put("Godan verb with 'ku' ending",                                  new String[]{"v5k",      "pos"});
        TAG_MAP.put("Godan verb - Iku/Yuku special class",                          new String[]{"v5k-s",    "pos"});
        TAG_MAP.put("Godan verb with 'mu' ending",                                  new String[]{"v5m",      "pos"});
        TAG_MAP.put("Godan verb with 'nu' ending",                                  new String[]{"v5n",      "pos"});
        TAG_MAP.put("Godan verb with 'ru' ending",                                  new String[]{"v5r",      "pos"});
        TAG_MAP.put("Godan verb with 'ru' ending (irregular verb)",                 new String[]{"v5r-i",    "pos"});
        TAG_MAP.put("Godan verb with 'su' ending",                                  new String[]{"v5s",      "pos"});
        TAG_MAP.put("Godan verb with 'tsu' ending",                                 new String[]{"v5t",      "pos"});
        TAG_MAP.put("Godan verb with 'u' ending",                                   new String[]{"v5u",      "pos"});
        TAG_MAP.put("Godan verb with 'u' ending (special class)",                   new String[]{"v5u-s",    "pos"});
        TAG_MAP.put("Godan verb - Uru old class verb (old form of Eru)",            new String[]{"v5uru",    "pos"});
        TAG_MAP.put("verb unspecified",                                              new String[]{"v-unspec", "pos"});
        // MISC
        TAG_MAP.put("abbreviation",                                                  new String[]{"abbr",     "misc"});
        TAG_MAP.put("archaic",                                                       new String[]{"arch",     "misc"});
        TAG_MAP.put("children's language",                                           new String[]{"chn",      "misc"});
        TAG_MAP.put("colloquial",                                                    new String[]{"col",      "misc"});
        TAG_MAP.put("dated term",                                                    new String[]{"dated",    "misc"});
        TAG_MAP.put("derogatory",                                                    new String[]{"derog",    "misc"});
        TAG_MAP.put("euphemistic",                                                   new String[]{"euph",     "misc"});
        TAG_MAP.put("familiar language",                                             new String[]{"fam",      "misc"});
        TAG_MAP.put("female term or language",                                       new String[]{"fem",      "misc"});
        TAG_MAP.put("formal or literary term",                                       new String[]{"form",     "misc"});
        TAG_MAP.put("historical term",                                               new String[]{"hist",     "misc"});
        TAG_MAP.put("honorific or respectful (sonkeigo) language",                  new String[]{"hon",      "misc"});
        TAG_MAP.put("humble (kenjougo) language",                                   new String[]{"hum",      "misc"});
        TAG_MAP.put("idiomatic expression",                                          new String[]{"id",       "misc"});
        TAG_MAP.put("jocular, humorous term",                                        new String[]{"joc",      "misc"});
        TAG_MAP.put("male term or language",                                         new String[]{"male",     "misc"});
        TAG_MAP.put("manga slang",                                                   new String[]{"m-sl",     "misc"});
        TAG_MAP.put("Internet slang",                                                new String[]{"net-sl",   "misc"});
        TAG_MAP.put("obsolete term",                                                 new String[]{"obs",      "misc"});
        TAG_MAP.put("onomatopoeic or mimetic word",                                 new String[]{"on-mim",   "misc"});
        TAG_MAP.put("poetical term",                                                 new String[]{"poet",     "misc"});
        TAG_MAP.put("polite (teineigo) language",                                   new String[]{"pol",      "misc"});
        TAG_MAP.put("proverb",                                                       new String[]{"proverb",  "misc"});
        TAG_MAP.put("rare term",                                                     new String[]{"rare",     "misc"});
        TAG_MAP.put("sensitive",                                                     new String[]{"sens",     "misc"});
        TAG_MAP.put("slang",                                                         new String[]{"sl",       "misc"});
        TAG_MAP.put("word usually written using kana alone",                        new String[]{"uk",       "misc"});
        TAG_MAP.put("vulgar expression or word",                                    new String[]{"vulg",     "misc"});
        TAG_MAP.put("rude or X-rated term (not displayed in educational software)", new String[]{"X",        "misc"});
        TAG_MAP.put("yojijukugo",                                                    new String[]{"yoji",     "misc"});
        // FIELD
        TAG_MAP.put("agriculture",             new String[]{"agric",    "field"});
        TAG_MAP.put("anatomy",                 new String[]{"anat",     "field"});
        TAG_MAP.put("archeology",              new String[]{"archeol",  "field"});
        TAG_MAP.put("architecture",            new String[]{"archit",   "field"});
        TAG_MAP.put("art, aesthetics",         new String[]{"art",      "field"});
        TAG_MAP.put("astronomy",               new String[]{"astron",   "field"});
        TAG_MAP.put("audiovisual",             new String[]{"audvid",   "field"});
        TAG_MAP.put("aviation",                new String[]{"aviat",    "field"});
        TAG_MAP.put("baseball",                new String[]{"baseb",    "field"});
        TAG_MAP.put("biochemistry",            new String[]{"biochem",  "field"});
        TAG_MAP.put("biology",                 new String[]{"biol",     "field"});
        TAG_MAP.put("botany",                  new String[]{"bot",      "field"});
        TAG_MAP.put("boxing",                  new String[]{"boxing",   "field"});
        TAG_MAP.put("Buddhism",                new String[]{"Buddh",    "field"});
        TAG_MAP.put("business",                new String[]{"bus",      "field"});
        TAG_MAP.put("card games",              new String[]{"cards",    "field"});
        TAG_MAP.put("chemistry",               new String[]{"chem",     "field"});
        TAG_MAP.put("Chinese mythology",       new String[]{"chmyth",   "field"});
        TAG_MAP.put("Christianity",            new String[]{"Christn",  "field"});
        TAG_MAP.put("civil engineering",       new String[]{"civeng",   "field"});
        TAG_MAP.put("clothing",                new String[]{"cloth",    "field"});
        TAG_MAP.put("computing",               new String[]{"comp",     "field"});
        TAG_MAP.put("crystallography",         new String[]{"cryst",    "field"});
        TAG_MAP.put("dentistry",               new String[]{"dent",     "field"});
        TAG_MAP.put("ecology",                 new String[]{"ecol",     "field"});
        TAG_MAP.put("economics",               new String[]{"econ",     "field"});
        TAG_MAP.put("electricity, elec. eng.", new String[]{"elec",     "field"});
        TAG_MAP.put("electronics",             new String[]{"electr",   "field"});
        TAG_MAP.put("embryology",              new String[]{"embryo",   "field"});
        TAG_MAP.put("engineering",             new String[]{"engr",     "field"});
        TAG_MAP.put("entomology",              new String[]{"ent",      "field"});
        TAG_MAP.put("figure skating",          new String[]{"figskt",   "field"});
        TAG_MAP.put("film",                    new String[]{"film",     "field"});
        TAG_MAP.put("finance",                 new String[]{"finc",     "field"});
        TAG_MAP.put("fishing",                 new String[]{"fish",     "field"});
        TAG_MAP.put("food, cooking",           new String[]{"food",     "field"});
        TAG_MAP.put("gardening, horticulture", new String[]{"gardn",    "field"});
        TAG_MAP.put("genetics",                new String[]{"genet",    "field"});
        TAG_MAP.put("geography",               new String[]{"geogr",    "field"});
        TAG_MAP.put("geology",                 new String[]{"geol",     "field"});
        TAG_MAP.put("geometry",                new String[]{"geom",     "field"});
        TAG_MAP.put("go (game)",               new String[]{"go",       "field"});
        TAG_MAP.put("golf",                    new String[]{"golf",     "field"});
        TAG_MAP.put("grammar",                 new String[]{"gramm",    "field"});
        TAG_MAP.put("Greek mythology",         new String[]{"grmyth",   "field"});
        TAG_MAP.put("hanafuda",                new String[]{"hanaf",    "field"});
        TAG_MAP.put("horse racing",            new String[]{"horse",    "field"});
        TAG_MAP.put("Internet",                new String[]{"internet", "field"});
        TAG_MAP.put("Japanese mythology",      new String[]{"jpmyth",   "field"});
        TAG_MAP.put("kabuki",                  new String[]{"kabuki",   "field"});
        TAG_MAP.put("law",                     new String[]{"law",      "field"});
        TAG_MAP.put("linguistics",             new String[]{"ling",     "field"});
        TAG_MAP.put("logic",                   new String[]{"logic",    "field"});
        TAG_MAP.put("martial arts",            new String[]{"MA",       "field"});
        TAG_MAP.put("mahjong",                 new String[]{"mahj",     "field"});
        TAG_MAP.put("manga",                   new String[]{"manga",    "field"});
        TAG_MAP.put("mathematics",             new String[]{"math",     "field"});
        TAG_MAP.put("mechanical engineering",  new String[]{"mech",     "field"});
        TAG_MAP.put("medicine",                new String[]{"med",      "field"});
        TAG_MAP.put("meteorology",             new String[]{"met",      "field"});
        TAG_MAP.put("military",                new String[]{"mil",      "field"});
        TAG_MAP.put("mineralogy",              new String[]{"min",      "field"});
        TAG_MAP.put("mining",                  new String[]{"mining",   "field"});
        TAG_MAP.put("motorsport",              new String[]{"motor",    "field"});
        TAG_MAP.put("music",                   new String[]{"music",    "field"});
        TAG_MAP.put("noh",                     new String[]{"noh",      "field"});
        TAG_MAP.put("ornithology",             new String[]{"ornith",   "field"});
        TAG_MAP.put("paleontology",            new String[]{"paleo",    "field"});
        TAG_MAP.put("pathology",               new String[]{"pathol",   "field"});
        TAG_MAP.put("pharmacology",            new String[]{"pharm",    "field"});
        TAG_MAP.put("philosophy",              new String[]{"phil",     "field"});
        TAG_MAP.put("photography",             new String[]{"photo",    "field"});
        TAG_MAP.put("physics",                 new String[]{"physics",  "field"});
        TAG_MAP.put("physiology",              new String[]{"physiol",  "field"});
        TAG_MAP.put("politics",                new String[]{"politics", "field"});
        TAG_MAP.put("printing",                new String[]{"print",    "field"});
        TAG_MAP.put("professional wrestling",  new String[]{"prowres",  "field"});
        TAG_MAP.put("psychiatry",              new String[]{"psy",      "field"});
        TAG_MAP.put("psychoanalysis",          new String[]{"psyanal",  "field"});
        TAG_MAP.put("psychology",              new String[]{"psych",    "field"});
        TAG_MAP.put("railway",                 new String[]{"rail",     "field"});
        TAG_MAP.put("Roman mythology",         new String[]{"rommyth",  "field"});
        TAG_MAP.put("Shinto",                  new String[]{"Shinto",   "field"});
        TAG_MAP.put("shogi",                   new String[]{"shogi",    "field"});
        TAG_MAP.put("skiing",                  new String[]{"ski",      "field"});
        TAG_MAP.put("sports",                  new String[]{"sports",   "field"});
        TAG_MAP.put("statistics",              new String[]{"stat",     "field"});
        TAG_MAP.put("stock market",            new String[]{"stockm",   "field"});
        TAG_MAP.put("sumo",                    new String[]{"sumo",     "field"});
        TAG_MAP.put("surgery",                 new String[]{"surg",     "field"});
        TAG_MAP.put("telecommunications",      new String[]{"telec",    "field"});
        TAG_MAP.put("trademark",               new String[]{"tradem",   "field"});
        TAG_MAP.put("television",              new String[]{"tv",       "field"});
        TAG_MAP.put("veterinary terms",        new String[]{"vet",      "field"});
        TAG_MAP.put("video games",             new String[]{"vidg",     "field"});
        TAG_MAP.put("zoology",                 new String[]{"zool",     "field"});
        // KE_INF
        TAG_MAP.put("ateji (phonetic) reading",                       new String[]{"ateji", "ke_inf"});
        TAG_MAP.put("word containing irregular kana usage",           new String[]{"ik",    "ke_inf"});
        TAG_MAP.put("word containing irregular kanji usage",          new String[]{"iK",    "ke_inf"});
        TAG_MAP.put("irregular okurigana usage",                      new String[]{"io",    "ke_inf"});
        TAG_MAP.put("word containing out-dated kanji or kanji usage", new String[]{"oK",    "ke_inf"});
        TAG_MAP.put("rarely used kanji form",                         new String[]{"rK",    "ke_inf"});
        TAG_MAP.put("search-only kanji form",                         new String[]{"sK",    "ke_inf"});
        // DIAL
        TAG_MAP.put("Brazilian",    new String[]{"bra",  "dial"});
        TAG_MAP.put("Hokkaido-ben", new String[]{"hob",  "dial"});
        TAG_MAP.put("Kansai-ben",   new String[]{"ksb",  "dial"});
        TAG_MAP.put("Kantou-ben",   new String[]{"ktb",  "dial"});
        TAG_MAP.put("Kyoto-ben",    new String[]{"kyb",  "dial"});
        TAG_MAP.put("Kyuushuu-ben", new String[]{"kyu",  "dial"});
        TAG_MAP.put("Nagano-ben",   new String[]{"nab",  "dial"});
        TAG_MAP.put("Osaka-ben",    new String[]{"osb",  "dial"});
        TAG_MAP.put("Ryuukyuu-ben", new String[]{"rkb",  "dial"});
        TAG_MAP.put("Touhoku-ben",  new String[]{"thb",  "dial"});
        TAG_MAP.put("Tosa-ben",     new String[]{"tsb",  "dial"});
        TAG_MAP.put("Tsugaru-ben",  new String[]{"tsug", "dial"});
    }

    public String importWords() {
        log.info("=== JMdict import started ===");
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        // 1. Kanji map
        Map<String, Integer> kanjiMap = jdbcTemplate.query(
            "SELECT kanji_id, kanji_character FROM kanji WHERE kanji_character IS NOT NULL",
            rs -> {
                Map<String, Integer> map = new HashMap<>();
                while (rs.next()) map.put(rs.getString("kanji_character"), rs.getInt("kanji_id"));
                return map;
            }
        );
        log.info("Kanji préchargés : {}", kanjiMap.size());

        // 2. Pré-insérer tous les tags connus
        for (Map.Entry<String, String[]> e : TAG_MAP.entrySet()) {
            jdbcTemplate.update(
                "INSERT INTO tag (tag_code, tag_type, tag_label) VALUES (?, ?, ?) ON CONFLICT (tag_code, tag_type) DO NOTHING",
                e.getValue()[0], e.getValue()[1], e.getKey()
            );
        }

        // 3. Cache label → tag_id
        Map<String, Integer> tagCache = jdbcTemplate.query(
            "SELECT tag_id, tag_label FROM tag",
            rs -> {
                Map<String, Integer> map = new HashMap<>();
                while (rs.next()) map.put(rs.getString("tag_label"), rs.getInt("tag_id"));
                return map;
            }
        );
        log.info("Tags préchargés : {}", tagCache.size());

        // 4. Parse SAX
        int[] stats = {0, 0, 0}; // [mots, exemples, word_tags]
        try {
            InputStream is = getClass().getResourceAsStream("/data/JMdict_e_examp.xml");
            if (is == null) return "ERROR: JMdict_e_examp.xml introuvable dans resources/data/";

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(is, new JMdictHandler(jdbcTemplate, kanjiMap, tagCache, stats));

        } catch (Exception e) {
            log.error("Erreur pendant l'import JMdict", e);
            return "ERROR: " + e.getMessage();
        }

        String result = String.format(
            "JMdict import terminé — Mots: %d | Exemples: %d | Tags liés: %d",
            stats[0], stats[1], stats[2]
        );
        log.info(result);
        return result;
    }

    private static class JMdictHandler extends DefaultHandler {

        private final JdbcTemplate        jdbcTemplate;
        private final Map<String, Integer> kanjiMap;
        private final Map<String, Integer> tagCache;
        private final int[]               stats;

        private boolean inKEle    = false;
        private boolean inREle    = false;
        private boolean inSense   = false;
        private String  glossLang = "eng";
        private String  sentLang  = "eng";

        private final StringBuilder buf = new StringBuilder();

        private final List<String>   kebList     = new ArrayList<>();
        private final List<String>   rebList     = new ArrayList<>();
        private final List<String>   glosses     = new ArrayList<>();
        private final List<String[]> exampleList = new ArrayList<>();
        private final Set<String>    entryTags   = new LinkedHashSet<>();

        private String exSrce, exForm, exJpn, exEng;

        private final List<Object[]> batchWordKanji = new ArrayList<>();
        private final List<Object[]> batchExamples  = new ArrayList<>();
        private final List<Object[]> batchWordTags  = new ArrayList<>();
        private static final int FLUSH_SIZE = 500;

        JMdictHandler(JdbcTemplate jdbcTemplate, Map<String, Integer> kanjiMap,
                      Map<String, Integer> tagCache, int[] stats) {
            this.jdbcTemplate = jdbcTemplate;
            this.kanjiMap     = kanjiMap;
            this.tagCache     = tagCache;
            this.stats        = stats;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            buf.setLength(0);
            switch (qName) {
                case "k_ele"   -> inKEle  = true;
                case "r_ele"   -> inREle  = true;
                case "sense"   -> inSense = true;
                case "gloss"   -> { glossLang = attrs.getValue("xml:lang"); if (glossLang == null) glossLang = "eng"; }
                case "ex_sent" -> { sentLang  = attrs.getValue("xml:lang"); if (sentLang  == null) sentLang  = "eng"; }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String text = buf.toString().trim();
            switch (qName) {
                case "entry"   -> { processEntry(); resetEntry(); }
                case "k_ele"   -> inKEle  = false;
                case "r_ele"   -> inREle  = false;
                case "sense"   -> inSense = false;
                case "keb"     -> { if (inKEle  && !text.isEmpty()) kebList.add(text); }
                case "reb"     -> { if (inREle  && !text.isEmpty()) rebList.add(text); }
                case "gloss"   -> { if (inSense && "eng".equals(glossLang) && !text.isEmpty()) glosses.add(text); }
                case "pos"     -> { if (inSense && !text.isEmpty()) entryTags.add(text); }
                case "misc"    -> { if (inSense && !text.isEmpty()) entryTags.add(text); }
                case "field"   -> { if (inSense && !text.isEmpty()) entryTags.add(text); }
                case "dial"    -> { if (inSense && !text.isEmpty()) entryTags.add(text); }
                case "ke_inf"  -> { if (inKEle  && !text.isEmpty()) entryTags.add(text); }
                case "ex_srce" -> exSrce = text;
                case "ex_text" -> exForm  = text;
                case "ex_sent" -> {
                    if ("jpn".equals(sentLang))      exJpn = text;
                    else if ("eng".equals(sentLang)) exEng = text;
                }
                case "example" -> {
                    if (exJpn != null && exEng != null)
                        exampleList.add(new String[]{exSrce, exForm, exJpn, exEng});
                    exSrce = null; exForm = null; exJpn = null; exEng = null;
                }
            }
            buf.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) { buf.append(ch, start, length); }

        private void processEntry() {
            if (rebList.isEmpty() && kebList.isEmpty()) return;
            if (glosses.isEmpty()) return;

            String wordJapanese = kebList.isEmpty() ? rebList.get(0) : kebList.get(0);
            String wordReading  = rebList.isEmpty()  ? wordJapanese  : rebList.get(0);
            String translation  = String.join("; ", glosses);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO word (word_japanese, word_pronunciation_hiragana, word_romaji, word_translation_en) VALUES (?, ?, ?, ?)",
                    new String[]{"word_id"}
                );
                ps.setString(1, wordJapanese);
                ps.setString(2, wordReading);
                ps.setString(3, "");
                ps.setString(4, translation);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key == null) return;
            int wordId = key.intValue();
            stats[0]++;

            Set<Integer> seenKanji = new HashSet<>();
            for (String keb : kebList) {
                int i = 0;
                while (i < keb.length()) {
                    int cp = keb.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    Integer kanjiId = kanjiMap.get(ch);
                    if (kanjiId != null && seenKanji.add(kanjiId))
                        batchWordKanji.add(new Object[]{wordId, kanjiId});
                    i += Character.charCount(cp);
                }
            }

            for (String[] ex : exampleList)
                batchExamples.add(new Object[]{wordId, ex[2], ex[3], ex[0], ex[1]});

            for (String tagText : entryTags) {
                Integer tagId = tagCache.get(tagText);
                if (tagId != null) batchWordTags.add(new Object[]{wordId, tagId});
            }

            if (stats[0] % FLUSH_SIZE == 0) {
                flushBatches();
                log.info("Progression : {} mots importés", stats[0]);
            }
        }

        private void flushBatches() {
            if (!batchWordKanji.isEmpty()) {
                jdbcTemplate.batchUpdate("INSERT INTO word_kanji (word_id, kanji_id) VALUES (?, ?)", batchWordKanji);
                batchWordKanji.clear();
            }
            if (!batchExamples.isEmpty()) {
                jdbcTemplate.batchUpdate(
                    "INSERT INTO word_example (word_id, we_japanese, we_english, we_tatoeba_id, we_form) VALUES (?, ?, ?, ?, ?)",
                    batchExamples);
                stats[1] += batchExamples.size();
                batchExamples.clear();
            }
            if (!batchWordTags.isEmpty()) {
                jdbcTemplate.batchUpdate("INSERT INTO word_tag (word_id, tag_id) VALUES (?, ?)", batchWordTags);
                stats[2] += batchWordTags.size();
                batchWordTags.clear();
            }
        }

        @Override public void endDocument() { flushBatches(); }

        private void resetEntry() {
            kebList.clear(); rebList.clear(); glosses.clear();
            exampleList.clear(); entryTags.clear();
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(""));
        }
    }
}