package net.rrm.ehour.ui.manage.user

import java.util
import java.util.Collections

import com.google.common.collect.Lists
import net.rrm.ehour.domain.User
import net.rrm.ehour.sort.UserComparator
import net.rrm.ehour.ui.common.border.GreyRoundedBorder
import net.rrm.ehour.ui.common.panel.AbstractBasePanel
import net.rrm.ehour.ui.common.panel.entryselector._
import net.rrm.ehour.ui.common.wicket.Event
import net.rrm.ehour.user.service.UserService
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.event.{Broadcast, IEvent}
import org.apache.wicket.model.ResourceModel
import org.apache.wicket.spring.injection.annot.SpringBean

class UserSelectionPanel(id: String, titleResourceKey: Option[String], filterUsers: (util.List[User]) => util.List[User]) extends AbstractBasePanel[LdapUserBackingBean](id) {
  def this(id: String, titleResourceKey: Option[String]) = this(id, titleResourceKey, xs => xs)

  val Self = this

  val hideInactiveFilter = new HideInactiveFilter()

  var entrySelectorPanel:EntrySelectorPanel = _

  @SpringBean
  protected var userService: UserService = _

  override def onInitialize() {
    super.onInitialize()

    val greyBorder = titleResourceKey match {
      case Some(resourceKey) => new GreyRoundedBorder("border", new ResourceModel(resourceKey))
      case None => new GreyRoundedBorder("border")
    }

    addOrReplace(greyBorder)

    val clickHandler = new EntrySelectorPanel.ClickHandler {
      def onClick(row: EntrySelectorData.EntrySelectorRow, target: AjaxRequestTarget) {
        val id = row.getId.asInstanceOf[Integer]

        send(Self.getPage, Broadcast.BREADTH, EntrySelectedEvent(id, target))
      }
    }

    entrySelectorPanel = new EntrySelectorPanel("entrySelectorFrame",
                                                createSelectorData(users),
                                                clickHandler,
                                                new ResourceModel("admin.user.hideInactive"))

    greyBorder.add(entrySelectorPanel)
  }

  private def createSelectorData(users: util.List[User]): EntrySelectorData = {
    val headers = Lists.newArrayList(new EntrySelectorData.Header("admin.user.lastName"),
                                    new EntrySelectorData.Header("admin.user.firstName"))

    import scala.collection.JavaConversions._
    val rows = for (user <- users) yield {
      val cells = Lists.newArrayList(user.getName, user.getUsername)
      new EntrySelectorData.EntrySelectorRow(cells, user.getUserId, user.isActive)
    }


    new EntrySelectorData(headers, rows)
  }

  override def onEvent(event: IEvent[_]) {
    def refresh(event: Event) {
      entrySelectorPanel.updateData(createSelectorData(users))
      entrySelectorPanel.reRender(event.target)
    }

    event.getPayload match {
      case event: EntryListUpdatedEvent => refresh(event)
      case event: InactiveFilterChangedEvent =>
        hideInactiveFilter.setHideInactive(event.hideInactiveFilter.isHideInactive)
        refresh(event)
      case _ =>
    }
  }

  private def users: util.List[User] = {
    val users: util.List[User] = filterUsers(if (hideInactiveFilter.isHideInactive) userService.getUsers() else userService.getUsers)
    Collections.sort(users, new UserComparator())
    users
  }
}
