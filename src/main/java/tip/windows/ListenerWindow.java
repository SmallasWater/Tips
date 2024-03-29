package tip.windows;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import tip.Main;
import tip.messages.*;
import tip.messages.defaults.*;
import tip.messages.defaults.BossBarMessage;
import tip.messages.defaults.BroadcastMessage;
import tip.messages.defaults.ChatMessage;
import tip.messages.defaults.NameTagMessage;
import tip.messages.defaults.ScoreBoardMessage;
import tip.messages.defaults.TipMessage;
import tip.utils.Api;
import tip.utils.PlayerConfig;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * @author SmallasWater
 */
public class ListenerWindow implements Listener {


    private LinkedHashMap<String,String> clickPlayer = new LinkedHashMap<>();

    /** 0 为 改变选中玩家(展示玩家列表) 1 为default触发 (不展示玩家列表) 2为自身 (不展示玩家列表)*/
    public static LinkedHashMap<String,Integer> CHOSE_TYPE = new LinkedHashMap<>();

    private LinkedHashMap<String, BaseMessage.BaseTypes> clickType = new LinkedHashMap<>();

    @EventHandler
    public void onWindow(PlayerFormRespondedEvent event){

        if (event.getResponse() != null) {
            Player p = event.getPlayer();
            int formId = event.getFormID();
            if (formId == CreateWindow.MENU
                    || formId == CreateWindow.SETTING
                    || formId == CreateWindow.CHOSE
                    || formId == CreateWindow.CHOSE_THEME){
                if (event.getWindow() instanceof FormWindowSimple) {
                    onListenerSimpleWindow(p, (FormWindowSimple) event.getWindow(), formId);
                }
                if (event.getWindow() instanceof FormWindowCustom) {
                    onListenerCustomWindow(p, (FormWindowCustom) event.getWindow(), formId);
                }

            }
        }
    }

    private void onListenerSimpleWindow(Player player,FormWindowSimple window,int id){
        if(id == CreateWindow.MENU){
            clickPlayer.put(player.getName(),window.getResponse().getClickedButton().getText());
            CreateWindow.sendSettingType(player);
        }
        if(id == CreateWindow.SETTING){
            if(ListenerWindow.CHOSE_TYPE.containsKey(player.getName())){
                BaseMessage.BaseTypes types = BaseMessage.getTypeByName(window.getResponse().getClickedButton().getText());
                if (types != null) {
                    clickType.put(player.getName(), types);
                    CreateWindow.sendSettingShow(player, types);
                }
            }else {
                String playerName = clickPlayer.get(player.getName());
                if (playerName != null) {
                    BaseMessage.BaseTypes types = BaseMessage.getTypeByName(window.getResponse().getClickedButton().getText());
                    if (types == null) {
                        CreateWindow.sendSetting(player);
                    } else {
                        clickType.put(player.getName(), types);
                        CreateWindow.sendSettingShow(player, types);
                    }
                } else {
                    CreateWindow.sendSetting(player);
                }
            }
        }
    }

    private void onListenerCustomWindow(Player player,FormWindowCustom window,int id){
        if(id == CreateWindow.CHOSE){
            String playerName = clickPlayer.get(player.getName());
            PlayerConfig config = Main.getInstance().getPlayerConfig(playerName);
            if(config == null){
                config = new PlayerConfig(playerName,new MessageManager(),null);
            }
            BaseMessage defaultMessage;
            BaseMessage baseMessage;
            BaseMessage.BaseTypes types = clickType.get(player.getName());
            String worldName = Api.getSettingLevels().get(window.getResponse()
                    .getDropdownResponse(0).getElementID());
            boolean open = window.getResponse().getToggleResponse(1);
            if(types != null){
                defaultMessage = Main.getInstance().getShowMessages().getMessageByTypeAndWorld(worldName,types.getType());
                if(defaultMessage == null){
                    return;
                }
                if(CHOSE_TYPE.get(player.getName()) == 0){
                    baseMessage = config.getMessage(worldName,types.getType());
                }else if(CHOSE_TYPE.get(player.getName()) == 1){
                    baseMessage = Api.getLevelDefaultMessage(worldName,types);
                }else{
                    config = Main.getInstance().getPlayerConfig(player.getName());
                    if(config == null){
                        config = new PlayerConfig(player.getName(),new MessageManager(),Main.getInstance().getTheme());
                    }
                    baseMessage = config.getMessage(worldName,types.getType());
                }

                BaseMessage remove = baseMessage;
                switch (types){
                    case CHAT_MESSAGE:
                        baseMessage = setChatBase(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    case SCORE_BOARD:
                        baseMessage = setScoreboard(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    case NAME_TAG:
                        baseMessage = setNameBase(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    case TIP:
                        baseMessage = setTipBase(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    case BOSS_BAR:
                        baseMessage = setBossBar(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    case BROADCAST:
                        baseMessage = setBroad(baseMessage,open,worldName,window,player,defaultMessage);
                        break;
                    default:break;
                }
                if(baseMessage != null){
                    if(CHOSE_TYPE.get(player.getName()) == 1){
                        Api.setLevelMessage(baseMessage);
                    }else {
                        config.setMessage(baseMessage);
                        config.save();
                    }
                    player.sendMessage("§7设置已保存..");
                }else{
                    config.removeMessage(remove);
                }
            }
        }
        if(id == CreateWindow.CHOSE_THEME){
            PlayerConfig config = Main.getInstance().getPlayerConfigInit(player.getName());
            if("关闭样式".equalsIgnoreCase(window.getResponse()
                    .getDropdownResponse(0).getElementContent())){
                config.setTheme(null);
                player.sendMessage("§2你已关闭样式");
                config.save();
                return;
            }
            String name = Main.getInstance().getThemeManager().getNames().get(window.getResponse()
                    .getDropdownResponse(0).getElementID());
            config.setTheme(name);
            player.sendMessage("§2你已切换到 "+window.getResponse()
                    .getDropdownResponse(0).getElementContent()+" §2样式");
            config.save();

        }
    }


    private BaseMessage setBroadCast(BaseMessage baseMessage, boolean open, String worldName, Player player, BroadcastMessage defaultMessage, String lines, int time){
        if(lines != null && !"".equals(lines)){
            LinkedList<String> line = new LinkedList<>(Arrays.asList(lines.split("&")));
            if(isEqualLine(line, defaultMessage.getMessages())){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    if(time ==  ((BroadcastMessage)baseMessage).getTime() ) {
                        player.sendMessage("§c未更改");
                        CreateWindow.sendSettingType(player);
                        return null;
                    }
                }
            }
            if(baseMessage != null) {
                ((BroadcastMessage) baseMessage).setMessages(line);
            }

        }else{
            ((BroadcastMessage) baseMessage).setMessages(defaultMessage.getMessages());
            if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                if(time ==  ((BroadcastMessage)baseMessage).getTime()) {
                    player.sendMessage("§7设置已初始化");
                    return null;
                }
            }
        }
        if(baseMessage != null) {
            ((BroadcastMessage) baseMessage).setTime(time);
        }
        return baseMessage;
    }

    private BaseMessage setChatBase(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new ChatMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((ChatMessage)defaultMessage).getMessage(),((ChatMessage)defaultMessage).isInWorld());
        }
        boolean inWorld = window.getResponse().getToggleResponse(2);
        String message = window.getResponse().getInputResponse(3);
        if(message != null && !"".equals(message)){
            if(inWorld == ((ChatMessage)baseMessage).isInWorld() &&((ChatMessage)baseMessage).getMessage().equalsIgnoreCase(message)){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    player.sendMessage("§c未更改");
                    CreateWindow.sendSettingType(player);
                    return null;
                }
            }else {
                ((ChatMessage) baseMessage).setInWorld(inWorld);
                ((ChatMessage) baseMessage).setMessage(message);
            }
        }else{
            ((ChatMessage)baseMessage).setMessage( ((ChatMessage)defaultMessage).getMessage());
            if(inWorld == ((ChatMessage)baseMessage).isInWorld()){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    player.sendMessage("§7设置已初始化");
                    return null;
                }
            }else{
                ((ChatMessage) baseMessage).setInWorld(inWorld);
            }
        }
        return baseMessage;
    }

    private BaseMessage setNameBase(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new NameTagMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((NameTagMessage)defaultMessage).getMessage());
        }
        String message = window.getResponse().getInputResponse(2);
        if(message != null && !"".equals(message)){
            if(((NameTagMessage)baseMessage).getMessage().equalsIgnoreCase(message)){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    player.sendMessage("§c未更改");
                    CreateWindow.sendSettingType(player);
                    return null;
                }
            }else {
                ((NameTagMessage) baseMessage).setMessage(message);
            }
        }else{
            ((NameTagMessage) baseMessage).setMessage( ((NameTagMessage) defaultMessage).getMessage());
            if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                player.sendMessage("§7设置已初始化");
                return null;
            }

        }

        baseMessage.setOpen(open);
        baseMessage.setWorldName(worldName);
        return baseMessage;
    }


    private BaseMessage setScoreboard(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new ScoreBoardMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((ScoreBoardMessage)defaultMessage).getTitle(),((ScoreBoardMessage)defaultMessage).getMessages());
        }
        String title = window.getResponse().getInputResponse(2);
        String lines = window.getResponse().getInputResponse(3);
        if(title != null && !"".equals(title)) {
            ((ScoreBoardMessage) baseMessage).setTitle(((ScoreBoardMessage)defaultMessage).getTitle());
            baseMessage =  setScoreBoardMessage(baseMessage, open, worldName, player, (ScoreBoardMessage) defaultMessage, lines,title);

        }else{
            title = ((ScoreBoardMessage)defaultMessage).getTitle();
            baseMessage = setScoreBoardMessage(baseMessage, open, worldName, player, (ScoreBoardMessage) defaultMessage, lines,title);
        }
        if(baseMessage != null){
            baseMessage.setOpen(open);
            baseMessage.setWorldName(worldName);
            ((ScoreBoardMessage)baseMessage).setTitle(title);
        }

        return baseMessage;

    }

    private BaseMessage setBossBarMessage(BaseMessage baseMessage, boolean open, String worldName, Player player, BossBarMessage defaultMessage, String lines, int time, boolean size) {
        if(lines != null && !"".equals(lines)){
            LinkedList<String> line = new LinkedList<>(Arrays.asList(lines.split("&")));
            if(isEqualLine(line, defaultMessage.getMessages())){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    if(time ==  ((BossBarMessage)baseMessage).getTime() && size ==  ((BossBarMessage)baseMessage).isSize()) {
                        player.sendMessage("§c未更改");
                        CreateWindow.sendSettingType(player);
                        return null;
                    }
                }
            }
            if(baseMessage != null) {
                ((BossBarMessage) baseMessage).setMessages(line);
            }

        }else{
            ((BossBarMessage) baseMessage).setMessages(defaultMessage.getMessages());
            if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                if(time ==  ((BossBarMessage)baseMessage).getTime() && size ==  ((BossBarMessage)baseMessage).isSize()) {
                    player.sendMessage("§7设置已初始化");
                    return null;
                }
            }
        }
        if(baseMessage != null) {
            ((BossBarMessage) baseMessage).setTime(time);
            ((BossBarMessage) baseMessage).setSize(size);
        }
        return baseMessage;
    }

    private boolean isEqualLine(LinkedList<String> str1,LinkedList<String> str2){
        return str1.toString().equalsIgnoreCase(str2.toString());
    }

    private BaseMessage setBroad(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new BroadcastMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((BroadcastMessage)defaultMessage).getTime(),((BroadcastMessage)defaultMessage).getMessages());
        }
        String timeString = window.getResponse().getInputResponse(2);
        String message = window.getResponse().getInputResponse(3);
        int time = ((BroadcastMessage) defaultMessage).getTime();
        if(timeString != null && !"".equals(timeString)) {
            try{
                time = Integer.parseInt(timeString);
            }catch (Exception ignore){}
            baseMessage = setBroadCast(baseMessage,open,worldName,player, (BroadcastMessage) defaultMessage,message,time);
        }else{
            baseMessage = setBroadCast(baseMessage,open,worldName,player, (BroadcastMessage) defaultMessage,message,time);
        }

        if(baseMessage != null){
            baseMessage.setOpen(open);
            baseMessage.setWorldName(worldName);
        }

        return baseMessage;

    }

    private BaseMessage setBossBar(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new BossBarMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((BossBarMessage)defaultMessage).getTime(),((BossBarMessage)defaultMessage).isSize(),((BossBarMessage)defaultMessage).getMessages());
        }
        String timeString = window.getResponse().getInputResponse(2);
        boolean size = window.getResponse().getToggleResponse(3);
        String message = window.getResponse().getInputResponse(4);
        int time = ((BossBarMessage) defaultMessage).getTime();
        if(timeString != null && !"".equals(timeString)) {
            try{
                time = Integer.parseInt(timeString);
            }catch (Exception ignore){}
            baseMessage = setBossBarMessage(baseMessage,open,worldName,player, (BossBarMessage) defaultMessage,message,time,size);
        }else{
            baseMessage = setBossBarMessage(baseMessage,open,worldName,player, (BossBarMessage) defaultMessage,message,time,size);
        }

        if(baseMessage != null){
            baseMessage.setOpen(open);
            baseMessage.setWorldName(worldName);
        }

        return baseMessage;

    }

    private BaseMessage setScoreBoardMessage(BaseMessage baseMessage, boolean open, String worldName, Player player, ScoreBoardMessage defaultMessage, String lines,String title) {
        if(lines != null && !"".equals(lines)){
            LinkedList<String> line = new LinkedList<>(Arrays.asList(lines.split("&")));
            if(isEqualLine(line, defaultMessage.getMessages())){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    if(title.equalsIgnoreCase(((ScoreBoardMessage)baseMessage).getTitle())) {
                        player.sendMessage("§c未更改");
                        CreateWindow.sendSettingType(player);
                        return null;
                    }
                }
            }
            ((ScoreBoardMessage)baseMessage).setMessages(line);
        }else{
            ((ScoreBoardMessage) baseMessage).setMessages(defaultMessage.getMessages());
            if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                if(title.equalsIgnoreCase(((ScoreBoardMessage)baseMessage).getTitle())) {
                    player.sendMessage("§7设置已初始化");
                    return null;
                }
            }
        }
        return baseMessage;
    }

    private BaseMessage setTipBase(BaseMessage baseMessage,boolean open,String worldName,FormWindowCustom window,Player player,BaseMessage defaultMessage){
        if(baseMessage == null){
            baseMessage = new TipMessage(defaultMessage.getWorldName(),defaultMessage.isOpen(),((TipMessage)defaultMessage).getShowType(),((TipMessage)defaultMessage).getMessage());
        }
        int showType = window.getResponse().getDropdownResponse(2).getElementID();
        String message = window.getResponse().getInputResponse(3);
        if(message != null && !"".equals(message)){
            if(((TipMessage)baseMessage).getMessage().equalsIgnoreCase(message)){
                if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                    if(showType == ((TipMessage)baseMessage).getShowType()) {
                        player.sendMessage("§c未更改");
                        CreateWindow.sendSettingType(player);
                        return null;
                    }
                }
            }
            ((TipMessage) baseMessage).setMessage(message);
            ((TipMessage) baseMessage).setType(showType);
        }else{
            ((TipMessage)baseMessage).setMessage(((TipMessage)defaultMessage).getMessage());
            if(open == baseMessage.isOpen() && worldName.equalsIgnoreCase(baseMessage.getWorldName())) {
                if(showType == ((TipMessage)baseMessage).getShowType()) {
                    player.sendMessage("§7设置已初始化");
                    return null;
                }
            }
            ((TipMessage) baseMessage).setType(showType);
        }
        baseMessage.setOpen(open);
        baseMessage.setWorldName(worldName);
        return baseMessage;
    }
}
