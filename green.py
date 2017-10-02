import datetime
import random
import os

# from heavy import special_commit


def modify():
    file = open('zero.md', 'r')
    flag = file.readline()
    file.close()
    file = open('zero.md', 'w+')
    if flag == '0':
        file.write('1')
    else:
        file.write('0')
    file.close()


def commit():
    os.system('git commit -a -m "Code update"')


def set_sys_time(year, month, day):
    os.system("date " + str(year)+"-"+str(month)+"-"+str(day))


def trick_commit(year, month, day):
    set_sys_time(year, month, day)
    t = random.randint(1,5)
    while (t > 0):
        modify()
        commit()
        t = t-1


def daily_commit(start_date, end_date):
    for i in range((end_date - start_date).days + 1):
        cur_date = start_date + datetime.timedelta(days=i)
        trick_commit(cur_date.year, cur_date.month, cur_date.day)


if __name__ == '__main__':
    daily_commit(datetime.date(2017, 10, 2), datetime.date(2019, 1, 11))
