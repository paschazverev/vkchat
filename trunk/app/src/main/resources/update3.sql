drop view dialog_v;

create view dialog_v as
select dialog._id _id,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       dialog.title title,
       message._id message_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo
  from dialog
  left join profile on dialog.user_id = profile._id
  left join message on message._id = dialog.last_message_id
  where dialog.last_message_id is not null;
