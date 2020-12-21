package org.golde.discordbot.supportserver;

import java.util.List;

import org.golde.discordbot.shared.ESSBot;
import org.golde.discordbot.shared.command.chatmod.ChatModCommand;
import org.golde.discordbot.shared.command.guildmod.GuildModCommand;
import org.golde.discordbot.shared.command.owner.OwnerCommand;
import org.golde.discordbot.shared.db.AbstractDBTranslation;
import org.golde.discordbot.shared.event.EventBase;
import org.golde.discordbot.supportserver.command.chatmod.CommandAddBlockedUrl;
import org.golde.discordbot.supportserver.command.chatmod.CommandBlockedUrls;
import org.golde.discordbot.supportserver.command.chatmod.CommandLookup;
import org.golde.discordbot.supportserver.command.chatmod.CommandMute;
import org.golde.discordbot.supportserver.command.chatmod.CommandPruneChat;
import org.golde.discordbot.supportserver.command.chatmod.CommandRemoveBlockedUrl;
import org.golde.discordbot.supportserver.command.chatmod.CommandUnmute;
import org.golde.discordbot.supportserver.command.chatmod.CommandUserHistory;
import org.golde.discordbot.supportserver.command.chatmod.CommandWarn;
import org.golde.discordbot.supportserver.command.guildmod.CommandBan;
import org.golde.discordbot.supportserver.command.guildmod.CommandKick;
import org.golde.discordbot.supportserver.command.guildmod.CommandToggleRole;
import org.golde.discordbot.supportserver.command.owner.CommandConvertOldDatabase;
import org.golde.discordbot.supportserver.command.owner.CommandUnban;
import org.golde.discordbot.supportserver.database.Offence;
import org.golde.discordbot.supportserver.event.BlockedUrlsPreventer;
import org.golde.discordbot.supportserver.event.ClientInvitesNeedsToBeBetter;
import org.golde.discordbot.supportserver.event.IHateKids2point0;
import org.golde.discordbot.supportserver.event.IPGrabberPrevention;
import org.golde.discordbot.supportserver.event.MiscModLog;
import org.golde.discordbot.supportserver.event.MuteManager;
import org.golde.discordbot.supportserver.event.StopChattingInTheWrongChannelsPls;
import org.golde.discordbot.supportserver.event.TryToFindIntrestingFiles;
import org.golde.discordbot.supportserver.util.ModLog;
import org.golde.discordbot.supportserver.util.ModLog.ModAction;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class ModerationBot extends ESSBot {

	public static void main(String[] args) throws Exception {
		new ModerationBot().run();
	}

	@Override
	public String getPrefix() {
		return ";";
	}

	@Override
	public void registerEventListeners(List<EventBase> events) {
		events.add(new IPGrabberPrevention(this));
		events.add(new MiscModLog(this));
		events.add(new TryToFindIntrestingFiles(this));
		events.add(new StopChattingInTheWrongChannelsPls(this));
		events.add(new ClientInvitesNeedsToBeBetter(this));
		events.add(new MuteManager(this));
		events.add(new IHateKids2point0(this));
		events.add(new BlockedUrlsPreventer(this));
	}

	@Override
	public void registerChatModCommand(List<ChatModCommand> cmds) {
		cmds.add(new CommandLookup(this));
		cmds.add(new CommandMute(this));
		cmds.add(new CommandUnmute(this));
		cmds.add(new CommandWarn(this));
		cmds.add(new CommandPruneChat(this));
		cmds.add(new CommandUserHistory(this));
		cmds.add(new CommandAddBlockedUrl(this));
		cmds.add(new CommandBlockedUrls(this));
		cmds.add(new CommandRemoveBlockedUrl(this));
	}

	@Override
	public void registerGuildModCommand(List<GuildModCommand> cmds) {
		cmds.add(new CommandKick(this));
		cmds.add(new CommandBan(this));
		cmds.add(new CommandToggleRole(this));
	}

	@Override
	public void registerOwnerCommand(List<OwnerCommand> cmds) {
		cmds.add(new CommandUnban(this));
		cmds.add(new CommandConvertOldDatabase(this));
		//cmds.add(new CommandRemoveAction(this));
	}

	@Override
	public void registerDatabaseTranslations(List<Class<? extends AbstractDBTranslation>> dbt) {
		dbt.add(Offence.class);
		super.registerDatabaseTranslations(dbt);
	}

	@Deprecated
	@Override
	public void onReady() {
		startExpireTimerForOffences();
	}

	private void startExpireTimerForOffences() {

		new Thread() {

			@Override
			public void run() {

				while(true) {


					List<Offence> exiredOffences = Offence.getExpiredOffences(ModerationBot.this);
					//System.out.println("Tick: " + exiredOffences.size());

					for(Offence off : exiredOffences) {

						System.out.println("Expired: " + off.toString());

						off.setHiddenBotInstance(ModerationBot.this);
						off.setExpired();

						Guild g = ModerationBot.this.getGuild();

						if(off.getType() == ModAction.MUTE) {

							MuteManager.unmuteUser(ModerationBot.this, off.getOffender(), ModerationBot.this.getJda().getSelfUser().getIdLong(), "Automatically unmuted");

						}
						else if(off.getType() == ModAction.BAN) {	
							Long offenceId = Offence.addOffence(ModerationBot.this, new Offence(off.getOffender(), ModerationBot.this.getJda().getSelfUser().getIdLong(), ModAction.UNBAN, "Automatically unbanned"));
							MessageEmbed actionEmbed = ModLog.getActionTakenEmbed(
									ModerationBot.this,
									ModAction.UNBAN, 
									ModerationBot.this.getJda().getSelfUser(), 
									new String[][] {
										new String[] {"Offender: ", "<@" + off.getOffender() + ">"}, 
										new String[] {"Reason:", "Automatically unbanned"},
										new String[] {"Offence ID:", Long.toString(offenceId)},
									}
									);
							ModLog.log(g, actionEmbed);
							g.unban(Long.toString(off.getOffender())).queue();
						}

					}


					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}


			}

		}.start();

	}

}
