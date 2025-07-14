package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.MemberDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;

import static com.example.splitmoneybot.constant.BotConstant.*;

@Service
@Slf4j
public class CalculateService {

    public int avg(Collection<Integer> list) {
        return list.stream()
                .reduce(Integer::sum)
                .map(integer -> integer / list.size())
                .orElse(0);
    }

    public String splitMoney(Map<MemberDto, Integer> map) {
        int avg = avg(map.values());
        Deque<MemberDto> lessAvgStack = new ArrayDeque<>();
        Deque<MemberDto> biggerAvgStack = new ArrayDeque<>();
        fillStacks(map, avg, lessAvgStack, biggerAvgStack);

        StringBuilder sb = new StringBuilder();
        while (!lessAvgStack.isEmpty() && !biggerAvgStack.isEmpty()) {
            MemberDto memberFrom = lessAvgStack.pop();
            MemberDto memberTo = biggerAvgStack.pop();

            int memberFromMoney = memberFrom.getMoney();
            int memberToMoney = memberTo.getMoney();

            int count = 0;
            while (memberFromMoney < avg && memberToMoney > avg) {
                count++;
                memberFromMoney ++;
                memberToMoney--;
            }
            sb.append(memberFrom.getName()).append(" -> ").append(memberTo.getName())
                    .append(" ").append(MONEY_EMOJI).append(" ").append(count).append("\n");

            if (memberFromMoney == avg && memberToMoney == avg) {
                continue;
            }
            if (memberFromMoney == avg) {
                memberTo.setMoney(memberToMoney);
                biggerAvgStack.addFirst(memberTo);
            }
            if (memberToMoney == avg) {
                memberFrom.setMoney(memberFromMoney);
                lessAvgStack.addFirst(memberFrom);
            }
        }
        return sb.isEmpty() ? "Не чего делить" : sb.toString();
    }

    private void fillStacks(Map<MemberDto, Integer> map, int avg,
                            Deque<MemberDto> lessAvgStack, Deque<MemberDto> biggerAvgStack) {
        for (Map.Entry<MemberDto, Integer> entry : map.entrySet()) {
            if (entry.getValue() < avg) {
                lessAvgStack.add(entry.getKey());
            }
            if (entry.getValue() > avg) {
                biggerAvgStack.add(entry.getKey());
            }
        }
    }
}
