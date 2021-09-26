create table booking
(
    id         uuid default random_uuid() primary key,
    date       date not null,
    booking_id uuid not null
);

create unique index unique_booking_date on booking(date);

create table guest
(
    id uuid default random_uuid() primary key,
    email varchar not null unique,
    name varchar not null
);

create table booking_to_guest
(
    id         uuid default random_uuid() primary key,
    booking_id uuid not null,
    guest_id   uuid not null,
--     foreign key (booking_id) references booking(booking_id),
--     this should be enabled, but it turned out that h2 creates unique index when you create foreign key
    foreign key (guest_id) references guest(id)
);