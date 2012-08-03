drop view dialog_v;

create view dialog_v as
select dialog._id _id,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       dialog.title title,
       dialog.last_message_id last_message_id,
       dialog.user_ids user_ids,
       message._id message_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online
  from dialog
  left join profile on dialog.user_id = profile._id
  left join message on message._id = dialog.last_message_id;

drop view message_v;

create view message_v as
select message._id _id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online
  from message
  left join profile on message.writer_id = profile._id;

drop view message_dialog_v;

create view message_dialog_v as
select message._id _id,
       message._id message_id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       dialog.title title,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       profile.online online
  from message
  left join profile on message.writer_id = profile._id
  left join dialog on dialog._id = message.dialog_id;