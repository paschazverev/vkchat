drop view dialog_v;

create view dialog_v as
select dialog._id _id,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       dialog.title title,
       dialog.last_message_id last_message_id,
       dialog.user_ids user_ids,
       dialog.regular regular,
       dialog.search search,
       message._id message_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online,
       writer.first_name writer_first_name,
       writer.last_name writer_last_name
  from dialog
  left join profile on dialog.user_id = profile._id
  left join message on message._id = dialog.last_message_id
  left join profile writer on writer._id = message.writer_id;
