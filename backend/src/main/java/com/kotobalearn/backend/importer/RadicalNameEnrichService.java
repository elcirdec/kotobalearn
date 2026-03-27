package com.kotobalearn.backend.importer;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Enrichit les composants KRADFILE (rad_type = 'component')
 * avec les noms, sens et traits des radicaux KanjiAlive.
 * Utilise le fichier japanese-radicals.csv.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RadicalNameEnrichService {

    private final DataSource dataSource;

    private static final String CSV_PATH = "/data/japanese-radicals.csv";

    public String enrichRadicalNames() throws Exception {
        log.info("Démarrage de l'enrichissement des noms de radicaux...");

        // 1. Charger le mapping Kangxi → CJK
        Map<String, String> kangxiToCjk = buildKangxiToCjkMap();

        // 2. Lire le CSV et construire une map caractère → (romaji, meaning, strokes)
        Map<String, RadicalInfo> csvData = readCsv(kangxiToCjk);

        addManualRadicals(csvData);

        // 3. Mettre à jour la table radical pour les composants actifs
        int updated = updateRadicals(csvData);

        String result = "Enrichissement terminé – " + updated + " composants mis à jour.";
        log.info(result);
        return result;
    }

    private Map<String, RadicalInfo> readCsv(Map<String, String> kangxiToCjk) throws Exception {
        Map<String, RadicalInfo> map = new HashMap<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {

            reader.readNext(); // skip header
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 6) continue;
                String kangxiChar = clean(row[2]); // radical character (Kangxi)
                String romaji = clean(row[5]);
                String meaning = clean(row[3]);
                Integer strokes = parseIntSafe(row[1]);

                if (kangxiChar.isEmpty() || romaji.isEmpty()) continue;

                String cjkChar = kangxiToCjk.get(kangxiChar);
                if (cjkChar == null) {
                    // Ignorer les radicaux Kangxi sans équivalent CJK connu
                    continue;
                }

                map.put(cjkChar, new RadicalInfo(romaji, meaning, strokes));
            }
        }
        log.info("{} entrées lues depuis le CSV (après conversion)", map.size());
        return map;
    }

    private int updateRadicals(Map<String, RadicalInfo> csvData) throws Exception {
        int updated = 0;
        String sql = """
            UPDATE radical
            SET rad_name_romaji = ?,
                rad_meaning_english = ?,
                rad_strokes = COALESCE(rad_strokes, ?)
            WHERE rad_type = 'component'
              AND rad_character = ?
              AND EXISTS (SELECT 1 FROM kanji_component WHERE radical_id = rad_id)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, RadicalInfo> entry : csvData.entrySet()) {
                String character = entry.getKey();
                RadicalInfo info = entry.getValue();
                ps.setString(1, info.romaji);
                ps.setString(2, info.meaning);
                ps.setInt(3, info.strokes != null ? info.strokes : 0);
                ps.setString(4, character);
                ps.addBatch();
            }
            int[] res = ps.executeBatch();
            for (int r : res) if (r > 0) updated++;
        }
        return updated;
    }

    /**
     * Construit un mapping des 214 radicaux Kangxi (U+2F00..U+2FD5) vers leurs
     * équivalents CJK unifiés (U+4E00..U+4ED5). Ajoute manuellement les variantes
     * courantes (⺅ → 亻, etc.).
     */
    private Map<String, String> buildKangxiToCjkMap() {
    Map<String, String> map = new HashMap<>();

    // Radicaux Kangxi (U+2F00..U+2FD5) → CJK unifiés (liste exacte)
    map.put("⼀", "一");
    map.put("⼁", "丨");
    map.put("⼂", "丶");
    map.put("⼃", "丿");
    map.put("⼄", "乙");
    map.put("⼅", "亅");
    map.put("⼆", "二");
    map.put("⼇", "亠");
    map.put("⼈", "人");
    map.put("⼉", "儿");
    map.put("⼊", "入");
    map.put("⼋", "八");
    map.put("⼌", "冂");
    map.put("⼍", "冖");
    map.put("⼎", "冫");
    map.put("⼏", "几");
    map.put("⼐", "凵");
    map.put("⼑", "刀");
    map.put("⼒", "力");
    map.put("⼓", "勹");
    map.put("⼔", "匕");
    map.put("⼕", "匚");
    map.put("⼖", "匸");
    map.put("⼗", "十");
    map.put("⼘", "卜");
    map.put("⼙", "卩");
    map.put("⼚", "厂");
    map.put("⼛", "厶");
    map.put("⼜", "又");
    map.put("⼝", "口");
    map.put("⼞", "囗");
    map.put("⼟", "土");
    map.put("⼠", "士");
    map.put("⼡", "夂");
    map.put("⼢", "夊");
    map.put("⼣", "夕");
    map.put("⼤", "大");
    map.put("⼥", "女");
    map.put("⼦", "子");
    map.put("⼧", "宀");
    map.put("⼨", "寸");
    map.put("⼩", "小");
    map.put("⼪", "尢");
    map.put("⼫", "尸");
    map.put("⼬", "屮");
    map.put("⼭", "山");
    map.put("⼮", "川");
    map.put("⼯", "工");
    map.put("⼰", "己");
    map.put("⼱", "巾");
    map.put("⼲", "干");
    map.put("⼳", "幺");
    map.put("⼴", "广");
    map.put("⼵", "廴");
    map.put("⼶", "廾");
    map.put("⼷", "弋");
    map.put("⼸", "弓");
    map.put("⼹", "彐");
    map.put("⼺", "彡");
    map.put("⼻", "彳");
    map.put("⼼", "心");
    map.put("⼽", "戈");
    map.put("⼾", "戶");
    map.put("⼿", "手");
    map.put("⽀", "支");
    map.put("⽁", "攴");
    map.put("⽂", "文");
    map.put("⽃", "斗");
    map.put("⽄", "斤");
    map.put("⽅", "方");
    map.put("⽆", "无");
    map.put("⽇", "日");
    map.put("⽈", "曰");
    map.put("⽉", "月");
    map.put("⽊", "木");
    map.put("⽋", "欠");
    map.put("⽌", "止");
    map.put("⽍", "歹");
    map.put("⽎", "殳");
    map.put("⽏", "毋");
    map.put("⽐", "比");
    map.put("⽑", "毛");
    map.put("⽒", "氏");
    map.put("⽓", "气");
    map.put("⽔", "水");
    map.put("⽕", "火");
    map.put("⽖", "爪");
    map.put("⽗", "父");
    map.put("⽘", "爻");
    map.put("⽙", "爿");
    map.put("⽚", "片");
    map.put("⽛", "牙");
    map.put("⽜", "牛");
    map.put("⽝", "犬");
    map.put("⽞", "玄");
    map.put("⽟", "玉");
    map.put("⽠", "瓜");
    map.put("⽡", "瓦");
    map.put("⽢", "甘");
    map.put("⽣", "生");
    map.put("⽤", "用");
    map.put("⽥", "田");
    map.put("⽦", "疋");
    map.put("⽧", "疒");
    map.put("⽨", "癶");
    map.put("⽩", "白");
    map.put("⽪", "皮");
    map.put("⽫", "皿");
    map.put("⽬", "目");
    map.put("⽭", "矛");
    map.put("⽮", "矢");
    map.put("⽯", "石");
    map.put("⽰", "示");
    map.put("⽱", "禸");
    map.put("⽲", "禾");
    map.put("⽳", "穴");
    map.put("⽴", "立");
    map.put("⽵", "竹");
    map.put("⽶", "米");
    map.put("⽷", "糸");
    map.put("⽸", "缶");
    map.put("⽹", "网");
    map.put("⽺", "羊");
    map.put("⽻", "羽");
    map.put("⽼", "老");
    map.put("⽽", "而");
    map.put("⽾", "耒");
    map.put("⽿", "耳");
    map.put("⾀", "聿");
    map.put("⾁", "肉");
    map.put("⾂", "臣");
    map.put("⾃", "自");
    map.put("⾄", "至");
    map.put("⾅", "臼");
    map.put("⾆", "舌");
    map.put("⾇", "舛");
    map.put("⾈", "舟");
    map.put("⾉", "艮");
    map.put("⾊", "色");
    map.put("⾋", "艸");
    map.put("⾌", "虍");
    map.put("⾍", "虫");
    map.put("⾎", "血");
    map.put("⾏", "行");
    map.put("⾐", "衣");
    map.put("⾑", "襾");
    map.put("⾒", "見");
    map.put("⾓", "角");
    map.put("⾔", "言");
    map.put("⾕", "谷");
    map.put("⾖", "豆");
    map.put("⾗", "豕");
    map.put("⾘", "豸");
    map.put("⾙", "貝");
    map.put("⾚", "赤");
    map.put("⾛", "走");
    map.put("⾜", "足");
    map.put("⾝", "身");
    map.put("⾞", "車");
    map.put("⾟", "辛");
    map.put("⾠", "辰");
    map.put("⾡", "辵");
    map.put("⾢", "邑");
    map.put("⾣", "酉");
    map.put("⾤", "釆");
    map.put("⾥", "里");
    map.put("⾦", "金");
    map.put("⾧", "長");
    map.put("⾨", "門");
    map.put("⾩", "阜");
    map.put("⾪", "隶");
    map.put("⾫", "隹");
    map.put("⾬", "雨");
    map.put("⾭", "靑");
    map.put("⾮", "非");
    map.put("⾯", "面");
    map.put("⾰", "革");
    map.put("⾱", "韋");
    map.put("⾲", "韭");
    map.put("⾳", "音");
    map.put("⾴", "頁");
    map.put("⾵", "風");
    map.put("⾶", "飛");
    map.put("⾷", "食");
    map.put("⾸", "首");
    map.put("⾹", "香");
    map.put("⾺", "馬");
    map.put("⾻", "骨");
    map.put("⾼", "高");
    map.put("⾽", "髟");
    map.put("⾾", "鬥");
    map.put("⾿", "鬯");
    map.put("⿀", "鬲");
    map.put("⿁", "鬼");
    map.put("⿂", "魚");
    map.put("⿃", "鳥");
    map.put("⿄", "鹵");
    map.put("⿅", "鹿");
    map.put("⿆", "麥");
    map.put("⿇", "麻");
    map.put("⿈", "黃");
    map.put("⿉", "黍");
    map.put("⿊", "黑");
    map.put("⿋", "黹");
    map.put("⿌", "黽");
    map.put("⿍", "鼎");
    map.put("⿎", "鼓");
    map.put("⿏", "鼠");
    map.put("⿐", "鼻");
    map.put("⿑", "齊");
    map.put("⿒", "齒");
    map.put("⿓", "龍");
    map.put("⿔", "龜");
    map.put("⿕", "龠");

    // Ajout des variantes (comme précédemment)
    String[][] variants = {
        {"⺅", "亻"}, {"⺌", "小"}, {"⺐", "尢"}, {"⺕", "彐"}, {"⺖", "忄"},
        {"⺘", "扌"}, {"⺡", "氵"}, {"⺨", "犭"}, {"⺩", "王"}, {"⺪", "疋"},
        {"⺫", "罒"}, {"⺭", "礻"}, {"⺮", "竹"}, {"⺷", "羊"}, {"⺹", "老"},
        {"⺼", "月"}, {"⺾", "艹"}, {"⻂", "衤"}, {"⻃", "覀"}, {"⻄", "西"},
        {"⻅", "見"}, {"⻆", "角"}, {"⻉", "貝"}, {"⻊", "足"}, {"⻋", "車"},
        {"⻌", "辶"}, {"⻍", "辶"}, {"⻏", "阝"}, {"⻑", "長"}, {"⻓", "長"},
        {"⻔", "門"}, {"⻖", "阝"}, {"⻗", "雨"}, {"⻘", "青"}, {"⻙", "韋"},
        {"⻚", "頁"}, {"⻛", "風"}, {"⻜", "飛"}, {"⻝", "食"}, {"⻟", "飠"},
        {"⻢", "馬"}, {"⻣", "骨"}, {"⻤", "鬼"}, {"⻥", "魚"}, {"⻦", "鳥"},
        {"⻨", "麥"}, {"⻩", "黄"}, {"⻫", "齊"}, {"⻬", "齊"}, {"⻭", "齒"},
        {"⻯", "龍"}, {"⻰", "龍"}, {"⻱", "龜"}
    };
    for (String[] v : variants) {
        map.put(v[0], v[1]);
    }
    
    return map;
    }

    private void addManualRadicals(Map<String, RadicalInfo> map) {
        // Normaliser les caractères pour éviter les problèmes Unicode
        map.put(Normalizer.normalize("｜", Normalizer.Form.NFC), new RadicalInfo("bou", "line", 1));
        map.put(Normalizer.normalize("ノ", Normalizer.Form.NFC), new RadicalInfo("no", "slash", 1));
        map.put(Normalizer.normalize("ハ", Normalizer.Form.NFC), new RadicalInfo("hachi", "eight", 2));
        map.put(Normalizer.normalize("マ", Normalizer.Form.NFC), new RadicalInfo("ma", "katakana ma", 2));
        map.put(Normalizer.normalize("ユ", Normalizer.Form.NFC), new RadicalInfo("yu", "katakana yu", 2));
        map.put(Normalizer.normalize("个", Normalizer.Form.NFC), new RadicalInfo("ko", "individual", 3));
        map.put(Normalizer.normalize("冂", Normalizer.Form.NFC), new RadicalInfo("kei", "wide", 2));
        map.put(Normalizer.normalize("刈", Normalizer.Form.NFC), new RadicalInfo("kari", "cut", 4));
        map.put(Normalizer.normalize("化", Normalizer.Form.NFC), new RadicalInfo("ka", "change", 4));
        map.put(Normalizer.normalize("并", Normalizer.Form.NFC), new RadicalInfo("hei", "average", 8)); // 並
        map.put(Normalizer.normalize("ヨ", Normalizer.Form.NFC), new RadicalInfo("yo", "katakana yo", 3));
        map.put(Normalizer.normalize("及", Normalizer.Form.NFC), new RadicalInfo("kyuu", "reach", 4));
        map.put(Normalizer.normalize("尚", Normalizer.Form.NFC), new RadicalInfo("shou", "esteem", 8));
        map.put(Normalizer.normalize("巛", Normalizer.Form.NFC), new RadicalInfo("kawa", "curving river", 3));
        map.put(Normalizer.normalize("已", Normalizer.Form.NFC), new RadicalInfo("i", "already", 3));
        map.put(Normalizer.normalize("幺", Normalizer.Form.NFC), new RadicalInfo("you", "short thread", 3));
        map.put(Normalizer.normalize("彑", Normalizer.Form.NFC), new RadicalInfo("kei", "pig head", 3));
        map.put(Normalizer.normalize("忙", Normalizer.Form.NFC), new RadicalInfo("bou", "busy", 6));
        map.put(Normalizer.normalize("扎", Normalizer.Form.NFC), new RadicalInfo("satsu", "stab", 4));
        map.put(Normalizer.normalize("汁", Normalizer.Form.NFC), new RadicalInfo("juu", "juice", 5));
        map.put(Normalizer.normalize("犯", Normalizer.Form.NFC), new RadicalInfo("han", "crime", 5));
        map.put(Normalizer.normalize("艾", Normalizer.Form.NFC), new RadicalInfo("gai", "artemisia", 5));
        map.put(Normalizer.normalize("込", Normalizer.Form.NFC), new RadicalInfo("komu", "included", 5));
        map.put(Normalizer.normalize("邦", Normalizer.Form.NFC), new RadicalInfo("hou", "country", 7));
        map.put(Normalizer.normalize("阡", Normalizer.Form.NFC), new RadicalInfo("sen", "thousand", 6));
        map.put(Normalizer.normalize("元", Normalizer.Form.NFC), new RadicalInfo("gen", "origin", 4));
        map.put(Normalizer.normalize("勿", Normalizer.Form.NFC), new RadicalInfo("motsu", "negation", 4));
        map.put(Normalizer.normalize("尤", Normalizer.Form.NFC), new RadicalInfo("yuu", "plausible", 4));
        map.put(Normalizer.normalize("屯", Normalizer.Form.NFC), new RadicalInfo("ton", "barracks", 4));
        map.put(Normalizer.normalize("巴", Normalizer.Form.NFC), new RadicalInfo("ha", "circle", 4));
        map.put(Normalizer.normalize("戸", Normalizer.Form.NFC), new RadicalInfo("to", "door", 4));
        map.put(Normalizer.normalize("攵", Normalizer.Form.NFC), new RadicalInfo("boku", "strike", 4));
        map.put(Normalizer.normalize("杰", Normalizer.Form.NFC), new RadicalInfo("ketsu", "hero", 8));
        map.put(Normalizer.normalize("王", Normalizer.Form.NFC), new RadicalInfo("ou", "king", 4));
        map.put(Normalizer.normalize("礼", Normalizer.Form.NFC), new RadicalInfo("rei", "gratitude", 5));
        map.put(Normalizer.normalize("冊", Normalizer.Form.NFC), new RadicalInfo("satsu", "book", 5));
        map.put(Normalizer.normalize("初", Normalizer.Form.NFC), new RadicalInfo("sho", "beginning", 7));
        map.put(Normalizer.normalize("巨", Normalizer.Form.NFC), new RadicalInfo("kyo", "giant", 5));
        map.put(Normalizer.normalize("母", Normalizer.Form.NFC), new RadicalInfo("haha", "mother", 5));
        map.put(Normalizer.normalize("牙", Normalizer.Form.NFC), new RadicalInfo("ga", "fang", 4));
        map.put(Normalizer.normalize("疔", Normalizer.Form.NFC), new RadicalInfo("tei", "furuncle", 7));
        map.put(Normalizer.normalize("禹", Normalizer.Form.NFC), new RadicalInfo("u", "legendary founder of the Xia Dynasty", 9));
        map.put(Normalizer.normalize("買", Normalizer.Form.NFC), new RadicalInfo("bai", "buy", 12));
        map.put(Normalizer.normalize("西", Normalizer.Form.NFC), new RadicalInfo("sei", "west", 6));
        map.put(Normalizer.normalize("麦", Normalizer.Form.NFC), new RadicalInfo("baku", "wheat", 11));
        map.put(Normalizer.normalize("免", Normalizer.Form.NFC), new RadicalInfo("men", "forgive", 8));
        map.put(Normalizer.normalize("奄", Normalizer.Form.NFC), new RadicalInfo("en", "cover", 8));
        map.put(Normalizer.normalize("岡", Normalizer.Form.NFC), new RadicalInfo("kou", "hill", 8));
        map.put(Normalizer.normalize("斉", Normalizer.Form.NFC), new RadicalInfo("sei", "equal", 8));
        map.put(Normalizer.normalize("品", Normalizer.Form.NFC), new RadicalInfo("hin", "goods", 9));
        map.put(Normalizer.normalize("竜", Normalizer.Form.NFC), new RadicalInfo("ryuu", "dragon", 10));
        map.put(Normalizer.normalize("滴", Normalizer.Form.NFC), new RadicalInfo("teki", "drop", 14));
        map.put(Normalizer.normalize("歯", Normalizer.Form.NFC), new RadicalInfo("shi", "tooth", 12));
        map.put(Normalizer.normalize("無", Normalizer.Form.NFC), new RadicalInfo("mu", "nothing", 12));
    }

    private String clean(String s) {
        return s == null ? "" : s.trim().replace("\uFEFF", "");
    }

    private Integer parseIntSafe(String s) {
        try {
            return Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record RadicalInfo(String romaji, String meaning, Integer strokes) {}
}