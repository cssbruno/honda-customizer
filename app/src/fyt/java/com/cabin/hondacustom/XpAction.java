package com.cabin.hondacustom;

/** Exact 0x4012A stock actions; see documents/research/xp-ready-actions.json. */
enum XpAction {
    MAINTENANCE(0x0e,R.string.xp_action_maintenance,R.string.xp_action_maintenance_help),
    RESTORE(0x0f,R.string.xp_action_restore,R.string.xp_action_restore_help),
    TPMS(0x11,R.string.xp_action_tpms,R.string.xp_action_tpms_help);
    final int key,title,help;
    XpAction(int key,int title,int help){this.key=key;this.title=title;this.help=help;}
    byte[] bytes(){return new byte[]{(byte)0xc6,0x02,(byte)key,0x00};}
}
