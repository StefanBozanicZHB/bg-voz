package zhb.company.bgvoz.data

/**
 * Ugrađena kopija reda vožnje BG:VOZ-a za relaciju Batajnica – Ovča.
 *
 * Aplikacija na svakom pokretanju preuzima i parsira zvanični PDF
 * ([TimetableRepository]); ovo je rezerva za prvi start bez mreže. Izvor:
 * https://www.srbvoz.rs/wp-content/redvoznje/rv_bg_voza_za_2025.pdf
 *
 * NE MENJATI RUČNO – regeneriše ga BundledTimetableGenerator
 * (./gradlew :app:regenerateBundledTimetable).
 */
object BundledTimetable {

    private val route: List<Station> = listOf(
        Station("BATAJNICA", "Batajnica"),
        Station("KAMENDIN", "Kamendin"),
        Station("ZEMUNSKO_POLJE", "Zemunsko Polje"),
        Station("ALTINA", "Altina"),
        Station("ZEMUN", "Zemun"),
        Station("TOSIN_BUNAR", "Tošin Bunar"),
        Station("NOVI_BEOGRAD", "Novi Beograd", stops = false),
        Station("BEOGRAD_CENTAR", "Beograd Centar"),
        Station("KARADJORDJEV_PARK", "Karađorđev Park"),
        Station("VUKOV_SPOMENIK", "Vukov Spomenik"),
        Station("PANCEVACKI_MOST", "Pančevački Most"),
        Station("KRNJACA_MOST", "Krnjača Most"),
        Station("KRNJACA_UKRSNICA", "Krnjača Ukrsnica"),
        Station("SEBES", "Sebeš"),
        Station("OVCA", "Ovča"),
    )

    private val holidays: Set<Triple<Int, Int, Int>> = setOf(
        Triple(2026, 1, 1),
        Triple(2026, 1, 2),
        Triple(2026, 1, 7),
        Triple(2026, 2, 15),
        Triple(2026, 2, 16),
        Triple(2026, 2, 17),
        Triple(2026, 4, 10),
        Triple(2026, 4, 12),
        Triple(2026, 4, 13),
        Triple(2026, 5, 1),
        Triple(2026, 5, 2),
        Triple(2026, 11, 11),
    )

    private val raw: List<Array<Any>> = listOf(
        arrayOf("8001", Direction.BATAJNICA_OVCA, false, listOf(360, 364, 366, 368, 372, 376, null, 383, 387, 391, 395, 399, 402, 405, 408)),
        arrayOf("8003", Direction.BATAJNICA_OVCA, true, listOf(390, 394, 396, 398, 402, 406, null, 413, 417, 421, 425, 429, 432, 435, 438)),
        arrayOf("8005", Direction.BATAJNICA_OVCA, false, listOf(430, 434, 436, 438, 442, 446, null, 453, 457, 461, 465, 469, 472, 475, 478)),
        arrayOf("8007", Direction.BATAJNICA_OVCA, true, listOf(450, 454, 456, 458, 462, 466, null, 473, 477, 481, 485, 489, 492, 495, 498)),
        arrayOf("8009", Direction.BATAJNICA_OVCA, false, listOf(480, 484, 486, 488, 492, 496, null, 503, 507, 511, 515, 519, 522, 525, 528)),
        arrayOf("8011", Direction.BATAJNICA_OVCA, true, listOf(510, 514, 516, 518, 522, 526, null, 533, 537, 541, 545, 549, 552, 555, 558)),
        arrayOf("8013", Direction.BATAJNICA_OVCA, false, listOf(537, 541, 543, 545, 558, 562, null, 569, 573, 577, 581, 585, 588, 591, 594)),
        arrayOf("8015", Direction.BATAJNICA_OVCA, true, listOf(570, 574, 576, 578, 582, 586, null, 593, 597, 601, 605, 609, 612, 615, 618)),
        arrayOf("8017", Direction.BATAJNICA_OVCA, false, listOf(610, 614, 616, 618, 622, 626, null, 633, 637, 641, 645, 649, 652, 655, 658)),
        arrayOf("8019", Direction.BATAJNICA_OVCA, false, listOf(680, 684, 686, 688, 692, 696, null, 703, 707, 711, 715, 719, 722, 725, 728)),
        arrayOf("8021", Direction.BATAJNICA_OVCA, false, listOf(720, 724, 726, 728, 732, 736, null, 743, 747, 751, 755, 759, 762, 765, 768)),
        arrayOf("8023", Direction.BATAJNICA_OVCA, false, listOf(755, 759, 761, 763, 767, 771, null, 778, 782, 786, 790, 794, 797, 800, 803)),
        arrayOf("8025", Direction.BATAJNICA_OVCA, false, listOf(840, 844, 846, 848, 852, 856, null, 863, 867, 871, 875, 879, 882, 885, 888)),
        arrayOf("8027", Direction.BATAJNICA_OVCA, true, listOf(870, 874, 876, 878, 882, 886, null, 893, 897, 901, 905, 909, 912, 915, 918)),
        arrayOf("8029", Direction.BATAJNICA_OVCA, false, listOf(910, 914, 916, 918, 922, 926, null, 933, 937, 941, 945, 949, 952, 955, 958)),
        arrayOf("8031", Direction.BATAJNICA_OVCA, true, listOf(930, 934, 936, 938, 942, 946, null, 953, 957, 961, 965, 969, 972, 975, 978)),
        arrayOf("8033", Direction.BATAJNICA_OVCA, false, listOf(960, 964, 966, 968, 972, 976, null, 983, 987, 991, 995, 999, 1002, 1005, 1008)),
        arrayOf("8035", Direction.BATAJNICA_OVCA, true, listOf(990, 994, 996, 998, 1002, 1006, null, 1013, 1017, 1021, 1025, 1029, 1032, 1035, 1038)),
        arrayOf("8037", Direction.BATAJNICA_OVCA, false, listOf(1030, 1034, 1036, 1038, 1042, 1046, null, 1053, 1057, 1061, 1065, 1069, 1072, 1075, 1078)),
        arrayOf("8039", Direction.BATAJNICA_OVCA, false, listOf(1050, 1054, 1056, 1058, 1062, 1066, null, 1073, 1077, 1081, 1085, 1089, 1092, 1095, 1098)),
        arrayOf("8041", Direction.BATAJNICA_OVCA, false, listOf(1110, 1114, 1116, 1118, 1122, 1126, null, 1133, 1137, 1141, 1145, 1149, 1152, 1155, 1158)),
        arrayOf("8043", Direction.BATAJNICA_OVCA, true, listOf(1150, 1154, 1156, 1158, 1162, 1166, null, 1173, 1177, 1181, 1185, 1189, 1192, 1195, 1198)),
        arrayOf("8045", Direction.BATAJNICA_OVCA, false, listOf(1200, 1204, 1206, 1208, 1212, 1216, null, 1223, 1227, 1231, 1235, 1239, 1242, 1245, 1248)),
        arrayOf("8000", Direction.OVCA_BATAJNICA, true, listOf(388, 384, 382, 380, 377, 373, null, 366, 363, 359, 356, 350, 348, 344, 340)),
        arrayOf("8002", Direction.OVCA_BATAJNICA, false, listOf(418, 414, 412, 410, 407, 403, null, 396, 393, 389, 386, 380, 378, 374, 370)),
        arrayOf("8004", Direction.OVCA_BATAJNICA, true, listOf(448, 444, 442, 440, 437, 433, null, 426, 423, 419, 416, 410, 408, 404, 400)),
        arrayOf("8006", Direction.OVCA_BATAJNICA, false, listOf(478, 474, 472, 470, 467, 463, null, 456, 453, 449, 446, 440, 438, 434, 430)),
        arrayOf("8008", Direction.OVCA_BATAJNICA, true, listOf(508, 504, 502, 500, 497, 493, null, 486, 483, 479, 476, 470, 468, 464, 460)),
        arrayOf("8010", Direction.OVCA_BATAJNICA, false, listOf(538, 534, 532, 530, 527, 523, null, 516, 513, 509, 506, 500, 498, 494, 490)),
        arrayOf("8012", Direction.OVCA_BATAJNICA, true, listOf(568, 564, 562, 560, 557, 553, null, 546, 543, 539, 536, 530, 528, 524, 520)),
        arrayOf("8014", Direction.OVCA_BATAJNICA, false, listOf(598, 594, 592, 590, 587, 583, null, 576, 573, 569, 566, 560, 558, 554, 550)),
        arrayOf("8016", Direction.OVCA_BATAJNICA, false, listOf(638, 634, 632, 630, 627, 623, null, 616, 613, 609, 606, 600, 598, 594, 590)),
        arrayOf("8018", Direction.OVCA_BATAJNICA, false, listOf(698, 694, 692, 690, 687, 683, null, 676, 673, 669, 666, 660, 658, 654, 650)),
        arrayOf("8020", Direction.OVCA_BATAJNICA, true, listOf(null, null, null, null, 716, 713, null, 706, 703, 699, 696, 690, 688, 684, 680)),
        arrayOf("8022", Direction.OVCA_BATAJNICA, false, listOf(778, 774, 772, 770, 767, 763, null, 756, 753, 749, 746, 740, 738, 734, 730)),
        arrayOf("8024", Direction.OVCA_BATAJNICA, false, listOf(838, 834, 832, 830, 827, 823, null, 816, 813, 809, 806, 800, 798, 794, 790)),
        arrayOf("8026", Direction.OVCA_BATAJNICA, false, listOf(878, 874, 872, 870, 867, 863, null, 856, 853, 849, 846, 840, 838, 834, 830)),
        arrayOf("8028", Direction.OVCA_BATAJNICA, true, listOf(928, 924, 922, 920, 917, 913, null, 906, 903, 899, 896, 890, 888, 884, 880)),
        arrayOf("8030", Direction.OVCA_BATAJNICA, false, listOf(958, 954, 952, 950, 947, 943, null, 936, 933, 929, 926, 920, 918, 914, 910)),
        arrayOf("8032", Direction.OVCA_BATAJNICA, true, listOf(988, 984, 982, 980, 977, 973, null, 966, 963, 959, 956, 950, 948, 944, 940)),
        arrayOf("8034", Direction.OVCA_BATAJNICA, false, listOf(1028, 1024, 1022, 1020, 1017, 1013, null, 1006, 1003, 999, 996, 990, 988, 984, 980)),
        arrayOf("8036", Direction.OVCA_BATAJNICA, true, listOf(1048, 1044, 1042, 1040, 1037, 1033, null, 1026, 1023, 1019, 1016, 1010, 1008, 1004, 1000)),
        arrayOf("8038", Direction.OVCA_BATAJNICA, false, listOf(1078, 1074, 1072, 1070, 1067, 1063, null, 1056, 1053, 1049, 1046, 1040, 1038, 1034, 1030)),
        arrayOf("8040", Direction.OVCA_BATAJNICA, false, listOf(1118, 1114, 1112, 1110, 1107, 1103, null, 1096, 1093, 1089, 1086, 1080, 1078, 1074, 1070)),
        arrayOf("8042", Direction.OVCA_BATAJNICA, true, listOf(null, null, null, null, 1156, 1153, null, 1146, 1143, 1139, 1136, 1130, 1128, 1124, 1120)),
        arrayOf("8044", Direction.OVCA_BATAJNICA, false, listOf(1198, 1194, 1192, 1190, 1187, 1183, null, 1176, 1173, 1169, 1166, 1160, 1158, 1154, 1150)),
        arrayOf("8046", Direction.OVCA_BATAJNICA, false, listOf(1228, 1224, 1222, 1220, 1217, 1213, null, 1206, 1203, 1199, 1196, 1190, 1188, 1184, 1180)),
        arrayOf("8048", Direction.OVCA_BATAJNICA, true, listOf(1268, 1264, 1262, 1260, 1257, 1253, null, 1246, 1243, 1239, 1236, 1230, 1228, 1224, 1220)),
        arrayOf("8050", Direction.OVCA_BATAJNICA, false, listOf(1318, 1314, 1312, 1310, 1307, 1303, null, 1296, 1293, 1289, 1286, 1280, 1278, 1274, 1270)),
        arrayOf("8340", Direction.OVCA_BATAJNICA, false, listOf(null, null, null, null, 1391, 1388, null, 1381, 1378, 1374, 1371, 1365, 1363, 1359, 1355)),
    )

    @Suppress("UNCHECKED_CAST")
    val data: TimetableData by lazy {
        TimetableData(
            route = route,
            trains = raw.map {
                Train(
                    number = it[0] as String,
                    direction = it[1] as Direction,
                    weekdayOnly = it[2] as Boolean,
                    times = it[3] as List<Int?>,
                    route = route,
                )
            },
            validFrom = "09.09.2026.",
            validTo = "12.12.2026.",
            holidays = holidays,
            origin = DataOrigin.BUNDLED,
            fetchedAt = null,
        )
    }
}
