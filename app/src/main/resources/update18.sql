alter table message add column deleted int;

drop view message_v;
drop view message_dialog_v;

create view message_v as
select message._id _id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       message.attachment attachment,
       message.deleted deleted,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online
  from message
  left join profile on message.writer_id = profile._id;

create view message_dialog_v as
select message._id _id,
       message._id message_id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       message.attachment attachment,
       message.deleted deleted,
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